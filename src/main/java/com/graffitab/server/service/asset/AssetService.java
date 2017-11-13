package com.graffitab.server.service.asset;

import com.graffitab.server.api.errors.EntityNotFoundException;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.image.ImageSizes;
import com.graffitab.server.service.image.ImageUtilsService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.util.GuidGenerator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 26/06/2016.
 */
@Log4j2
@Service
public class AssetService {

    private DatastoreService datastoreService;
    private HibernateDaoImpl<Asset, Long> assetDao;
    private TransactionUtils transactionUtils;
    private ImageUtilsService imageUtilsService;

    private ExecutorService assetOperationsExecutor = Executors.newFixedThreadPool(4);
    private Map<String, String> newAssetGuidToPreviousAssetGuidMap = new ConcurrentHashMap<>();

    @NonNull
    @Value("${filesystem.tempDir:/tmp}")
    public String FILE_SYSTEM_TEMP_ROOT;

    @PostConstruct
    public void init() {
        File file = new File(FILE_SYSTEM_TEMP_ROOT);
        if (!file.exists()) {
            file.mkdirs();
        }

        if (log.isDebugEnabled()) {
            log.debug("Temporary filesystem root is " + FILE_SYSTEM_TEMP_ROOT);
        }
    }

    @Autowired
    public AssetService(DatastoreService datastoreService, ImageUtilsService imageUtilsService,
                        HibernateDaoImpl<Asset, Long> assetDao, TransactionUtils transactionUtils) {
        this.datastoreService = datastoreService;
        this.imageUtilsService = imageUtilsService;
        this.transactionUtils = transactionUtils;
        this.assetDao = assetDao;
    }

    @Transactional(readOnly = true)
	public Asset getAsset(String assetGuid) {
    	Asset asset = findAssetByGuid(assetGuid);

		if (asset == null) {
			throw new EntityNotFoundException(ResultCode.ASSET_NOT_FOUND, "Could not find asset with guid " + assetGuid);
		}

		return asset;
	}

    public String transferAssetFile(TransferableStream transferableAsset, Long contentLength) {
        String assetGuid = GuidGenerator.generate();
        transferAssetToTemporaryArea(transferableAsset, assetGuid);
        return assetGuid;
    }

    private File transferAssetToTemporaryArea(TransferableStream transferableAsset, String assetGuid) {
        File tempFile = imageUtilsService.getTemporaryFile(assetGuid);

        if (log.isDebugEnabled()) {
            log.debug("Transferring multipart file to temporary store {}", tempFile.getAbsolutePath());
        }

        try {

            transferableAsset.transferTo(tempFile);

            if (log.isDebugEnabled()) {
                log.debug("Transferring file to temporary completed successfully {}", tempFile.getAbsolutePath());
            }
            return tempFile;
        } catch (FileNotFoundException e) {
            log.error("File not found: " + tempFile.getAbsolutePath(), e);
            throw new RestApiException(ResultCode.GENERAL_ERROR, "Cannot transfer to temporary file");
        } catch (IOException e) {
            log.error("General error transferring file", e);
            throw new RestApiException(ResultCode.GENERAL_ERROR, "Cannot transfer to temporary file");
        }
    }

    public void addPreviousAssetGuidMapping(String newAssetGuid, String previousAssetGuid) {
        newAssetGuidToPreviousAssetGuidMap.put(newAssetGuid, previousAssetGuid);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Asset findAssetByGuid(String assetGuid) {
        Query query = assetDao.createNamedQuery("Asset.findByGuid")
                              .setParameter("guid", assetGuid);
        return (Asset) query.uniqueResult();
    }

    public Runnable prepareAssetForDeferredProcessing(final String assetGuid) {
        return () -> deferredAssetProcessing(assetGuid);
    }

    private void deferredAssetProcessing(String assetGuid) {

        log.info("Processing asset with GUID " + assetGuid);

        // Resize and upload to Amazon S3
        ImageSizes imageSizes = imageUtilsService.generateAndUploadImagesForAsset(assetGuid);

        // Set as completed
        transactionUtils.executeInNewTransaction(() -> {
            Asset toUpdate = assetDao.findByUniqueField("guid", assetGuid);
            toUpdate.setState(Asset.AssetState.COMPLETED);
            toUpdate.setHeight(imageSizes.getHeight());
            toUpdate.setWidth(imageSizes.getWidth());
            toUpdate.setThumbnailHeight(imageSizes.getThumbnailHeight());
            toUpdate.setThumbnailWidth(imageSizes.getThumbnailWidth());
        });

        log.info("Processing of asset with GUID {} finished", assetGuid);

        // Delete previous asset in datastore -- This only works for a single node app, if we want
        // this to work clustered, the map needs to be in Redis
        String previousAssetGuid = newAssetGuidToPreviousAssetGuidMap.get(assetGuid);
        if (previousAssetGuid != null) {
            datastoreService.deleteAsset(previousAssetGuid);
            datastoreService.deleteAsset(previousAssetGuid + ImageUtilsService.ASSET_THUMBNAIL_SUFFIX);
            newAssetGuidToPreviousAssetGuidMap.remove(assetGuid);
        }
    }

    public void enqueueDeferredAssetProcessing(Runnable deferredAssetProcessingRunnable) {
        assetOperationsExecutor.submit(deferredAssetProcessingRunnable);
    }
}
