package com.graffitab.server.service.store;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class AmazonS3DatastoreService implements DatastoreService {

	@Value("${s3.bucketName:graffitab-eu1}")
	public String bucketName;
	public static String SUFFIX = "/";
	public static String ASSETS_ROOT_KEY = "assets";

	private static Logger LOG = LogManager.getLogger();

	private String AWS_SECRET_ENVVAR_NAME = "AWS_SECRET_KEY";
	private String AWS_KEY_ENVVAR_NAME = "AWS_ACCESS_KEY";

	private AmazonS3 amazonS3Client;

	@PostConstruct
	public void setupClient() {
		String awsSecret = System.getenv(AWS_SECRET_ENVVAR_NAME);
		String awsKey = System.getenv(AWS_KEY_ENVVAR_NAME);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Logging into Amazon S3 - AWS Key is {} - bucket is {}", awsKey, bucketName);
		}

		if (StringUtils.hasText(awsKey) && StringUtils.hasText(awsSecret)) {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsKey, awsSecret);
			amazonS3Client = new AmazonS3Client(awsCreds);
			LOG.info("Successfully logged into Amazon S3");
		} else {
			LOG.warn("Amazon S3 credentials are null -- The datastore won't work");
		}
	}

	@Override
	public void saveAsset(InputStream inputStream, long contentLength, String assetGuid) {
		LOG.debug("About to save asset " + assetGuid);

		String key = generateKey(assetGuid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Starting upload to Amazon S3, key is {}", key);
		}

		ObjectMetadata requestMetadata = new ObjectMetadata();
		requestMetadata.setContentLength(contentLength);
		requestMetadata.setContentType(MimeTypeUtils.IMAGE_JPEG_VALUE);

		PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, inputStream, requestMetadata).withCannedAcl(CannedAccessControlList.PublicRead);
		PutObjectResult result = amazonS3Client.putObject(putRequest);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Upload finished, ETag is {}", result.getETag());
			LOG.debug("Download link is {}", generateDownloadLink(assetGuid));
		}
	}

	@Override
	public void updateAsset(InputStream inputStream, long contentLength, String assetGuid) {
		saveAsset(inputStream, contentLength, assetGuid);
	}

	@Override
	public void deleteAsset(String assetGuid) {
		LOG.debug("About to delete asset " + assetGuid);

		String key = generateKey(assetGuid);

		amazonS3Client.deleteObject(new DeleteObjectRequest(bucketName, key));

		if (LOG.isDebugEnabled()) {
			LOG.debug("Deleted asset {}", key);
		}
	}

	@Override
	public String generateDownloadLink(String assetGuid) {
		return "http://" + bucketName + ".s3.amazonaws.com/" + generateKey(assetGuid);
	}

	private static String generateKey(String assetGuid) {
		return ASSETS_ROOT_KEY + "/" + assetGuid;
	}

	@SuppressWarnings("unused")
	private void createFolderInBucket(String folderName) {

		// create meta-data for your folder and set content-length to 0
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		// create empty content
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

		// create a PutObjectRequest passing the folder name suffixed by /
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
					folderName + SUFFIX, emptyContent, metadata);

		// send request to S3 to create folder
		PutObjectResult putObjectResult = amazonS3Client.putObject(putObjectRequest);

		if(LOG.isDebugEnabled()) {
			LOG.debug("Result of creation of folder in Amazon S3: hash " +
						putObjectResult.getMetadata().getContentMD5());
		}
	}

	@Override
	public String generateThumbnailLink(String assetGuid) {
		return "http://" + bucketName + ".s3.amazonaws.com/" + generateKey(assetGuid) + "_thumb";
	}

	public String getBucketName() {
		return bucketName;
	}
}
