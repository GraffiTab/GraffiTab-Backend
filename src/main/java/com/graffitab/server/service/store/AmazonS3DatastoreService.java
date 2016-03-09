package com.graffitab.server.service.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.UploadJobService;

@Service
public class AmazonS3DatastoreService implements DatastoreService {

	private static Logger LOG = LogManager.getLogger();

	private static String BUCKET_NAME = "graffitab-eu1"; // Single bucket for now
	private static String SUFFIX = "/";
	private static String USERS_ROOT_KEY = "users";

	private String AWS_SECRET_ENVVAR_NAME = "AWS_SECRET_KEY";
	private String AWS_KEY_ENVVAR_NAME = "AWS_ACCESS_KEY";

	private AmazonS3 amazonS3Client;

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private UploadJobService uploadJobService;

	@PostConstruct
	public void setupClient() {
		String awsSecret = System.getenv(AWS_SECRET_ENVVAR_NAME);
		String awsKey = System.getenv(AWS_KEY_ENVVAR_NAME);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Logging into Amazon S3 - AWS Key is {}", awsKey);
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
	public void saveAsset(InputStream inputStream, long contentLength, String userGuid, String assetGuid) {
		LOG.debug("About to save asset " + assetGuid);

		String key = generateKey(userGuid, assetGuid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Starting upload to Amazon S3, key is {}", key);
		}

		ObjectMetadata requestMetadata = new ObjectMetadata();
		requestMetadata.setContentLength(contentLength);

		PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, key, inputStream, requestMetadata).withCannedAcl(CannedAccessControlList.PublicRead);
		PutObjectResult result = amazonS3Client.putObject(putRequest);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Upload finished, ETag is {}", result.getETag());
			LOG.debug("Download link is {}", generateDownloadLink(userGuid, assetGuid));
		}
	}

	@Override
	public void updateAsset(InputStream inputStream, long contentLength, String userGuid, String assetGuid) {
		saveAsset(inputStream, contentLength, userGuid, assetGuid);
	}

	@Override
	public void deleteAsset(String userGuid, String assetGuid) {
		LOG.debug("About to delete asset " + assetGuid);

		String key = generateKey(userGuid, assetGuid);

		amazonS3Client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, key));

		if (LOG.isDebugEnabled()) {
			LOG.debug("Deleted asset {}", key);
		}
	}

	@Override
	public String generateDownloadLink(String userGuid, String assetGuid) {
		return "http://" + BUCKET_NAME + ".s3.amazonaws.com/" + generateKey(userGuid, assetGuid);
	}

	private String generateKey(String userGuid, String assetGuid) {
		return USERS_ROOT_KEY + "/" + userGuid + "/" + assetGuid;
	}

	// Unused for now.

	private static void createFolderInBucket(String folderName, AmazonS3Client amazonS3Client) {

		// create meta-data for your folder and set content-length to 0
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		// create empty content
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

		// create a PutObjectRequest passing the folder name suffixed by /
		PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
					folderName + SUFFIX, emptyContent, metadata);

		// send request to S3 to create folder
		PutObjectResult putObjectResult = amazonS3Client.putObject(putObjectRequest);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Result of creation of folder in Amazon S3: hash " +
						putObjectResult.getMetadata().getContentMD5());
		}
	}

	private void createFolderInBucket(String folderName) {

		// create meta-data for your folder and set content-length to 0
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		// create empty content
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

		// create a PutObjectRequest passing the folder name suffixed by /
		PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
					folderName + SUFFIX, emptyContent, metadata);

		// send request to S3 to create folder
		PutObjectResult putObjectResult = amazonS3Client.putObject(putObjectRequest);

		if(LOG.isDebugEnabled()) {
			LOG.debug("Result of creation of folder in Amazon S3: hash " +
						putObjectResult.getMetadata().getContentMD5());
		}
	}
}
