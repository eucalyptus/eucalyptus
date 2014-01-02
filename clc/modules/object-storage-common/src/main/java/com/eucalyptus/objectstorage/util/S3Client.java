package com.eucalyptus.objectstorage.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;

public class S3Client {
	private static final int CONNECTION_TIMEOUT_MS = 500;
	private S3ClientOptions ops;
	private AmazonS3Client s3Client;
	
	public S3Client(AWSCredentials credentials, boolean https) {
		ClientConfiguration config = new ClientConfiguration();
		config.setConnectionTimeout(CONNECTION_TIMEOUT_MS); //very short timeout
		Protocol protocol = https ? Protocol.HTTPS : Protocol.HTTP;
		config.setProtocol(protocol);
		s3Client = new AmazonS3Client(credentials, config);
		ops = new S3ClientOptions();
		s3Client.setS3ClientOptions(ops);
	}

	public void setUsePathStyle(boolean usePathStyle) {
		ops.setPathStyleAccess(usePathStyle);
		s3Client.setS3ClientOptions(ops);
	}

	public AmazonS3Client getS3Client() {
		return s3Client;
	}

	public void setS3Endpoint(String s3Endpoint) {
		s3Client.setEndpoint(s3Endpoint);
	}	
	
}