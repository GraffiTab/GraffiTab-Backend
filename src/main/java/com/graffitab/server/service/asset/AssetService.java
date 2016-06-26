package com.graffitab.server.service.asset;

import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.image.ImageSizes;
import com.graffitab.server.service.image.ImageUtilsService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.util.GuidGenerator;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.util.List;
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

    @Resource
    private DatastoreService datastoreService;

    @Resource
    private HibernateDaoImpl<Asset, Long> assetDao;

    @Resource
    private TransactionUtils transactionUtils;

    @Resource
    private ImageUtilsService imageUtilsService;

    private ExecutorService assetOperationsExecutor = Executors.newFixedThreadPool(4);

    private Map<String, String> newAssetGuidToPreviousAssetGuidMap = new ConcurrentHashMap<>();

    @Value("${filesystem.tempDir:/tmp}")
    private String FILE_SYSTEM_TEMP_ROOT;

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

    public String transferAssetFile(TransferableStream transferableAsset, Long contentLength) {
        String assetGuid = GuidGenerator.generate();
        transferAssetToTemporaryArea(transferableAsset, assetGuid);
        return assetGuid;
    }

    public File transferAssetToTemporaryArea(TransferableStream transferableAsset, String assetGuid) {
        File tempFile = getTemporaryFile(assetGuid);

        if (log.isDebugEnabled()) {
            log.debug("Transferring multipart file to temporary store {}", tempFile.getAbsolutePath());
        }

        try {

            transferableAsset.transferTo(tempFile);

            if (log.isDebugEnabled()) {
                log.debug("Transferring file to temporary completed successfully", tempFile.getAbsolutePath());
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

    public File getTemporaryFile(String temporaryFilename) {
        return new File(FILE_SYSTEM_TEMP_ROOT + File.separatorChar + temporaryFilename);
    }

    public String getTemporaryFilePath(String temporaryFilename) {
        return FILE_SYSTEM_TEMP_ROOT + File.separatorChar + temporaryFilename;
    }

    @Scheduled(fixedDelay = 1000)
    public void uploadAndResizeImagesForProcessingAssets() {

        // Get processing assets
        List<Asset> processingAssets = transactionUtils.executeInTransactionWithResult(() -> {
            Query query = assetDao.createNamedQuery("Asset.findInState")
                    .setParameter("state", Asset.AssetState.PROCESSING);
            return (List<Asset>) query.list();
        });

        if (processingAssets.size() > 0) {
            log.info("There are {} assets to process", processingAssets.size());
        }

        processingAssets.forEach((asset) -> {
            assetOperationsExecutor.submit(() -> {

                log.info("Processing asset with GUID " + asset.getGuid());

                // Set as uploading so it is not picked up by other threads
                transactionUtils.executeInNewTransaction(() -> {
                    Asset toUpdate = assetDao.find(asset.getId());
                    toUpdate.setState(Asset.AssetState.RESIZING);
                });

                // Resize and upload to Amazon S3
                ImageSizes imageSizes = imageUtilsService.generateAndUploadImagesForAsset(asset.getGuid());

                // Set as completed
                transactionUtils.executeInNewTransaction(() -> {
                    Asset toUpdate = assetDao.find(asset.getId());
                    toUpdate.setState(Asset.AssetState.COMPLETED);
                    toUpdate.setHeight(imageSizes.getHeight());
                    toUpdate.setWidth(imageSizes.getWidth());
                    toUpdate.setThumbnailHeight(imageSizes.getThumbnailHeight());
                    toUpdate.setThumbnailWidth(imageSizes.getThumbnailWidth());
                });

                log.info("Processing of asset with GUID {} finished", asset.getGuid());

                // Delete previous asset in datastore
                String previousAssetGuid = newAssetGuidToPreviousAssetGuidMap.get(asset.getGuid());
                if (previousAssetGuid != null) {
                    datastoreService.deleteAsset(previousAssetGuid);
                    datastoreService.deleteAsset(previousAssetGuid + ImageUtilsService.ASSET_THUMBNAIL_SUFFIX);
                    newAssetGuidToPreviousAssetGuidMap.remove(asset.getGuid());
                }
            });
        });
    }

    public void addPreviousAssetGuidMapping(String newAssetGuid, String previousAssetGuid) {
        newAssetGuidToPreviousAssetGuidMap.put(newAssetGuid, previousAssetGuid);
    }

    @Transactional(readOnly = true)
    public Asset findAssetByGuid(String assetGuid) {
        Query query = assetDao.createNamedQuery("Asset.findByGuid")
                              .setParameter("guid", assetGuid);
        return (Asset) query.uniqueResult();
    }
}
