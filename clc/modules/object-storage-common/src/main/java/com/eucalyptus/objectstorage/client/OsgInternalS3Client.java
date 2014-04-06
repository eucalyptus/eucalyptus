package com.eucalyptus.objectstorage.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;

/**
 * A convenience wrapper for an AWS Java SDK S3 Client that sets default
 * timeouts etc, options, etc
 *
 * This is specifically as needed for the OSG's internal use to various backends
 */
public class OsgInternalS3Client {
    private static final int CONNECTION_TIMEOUT_MS = 500; //500ms connection timeout, fail fast
    private static final int OSG_SOCKET_TIMEOUT_MS = 10 * 1000; //10 sec socket timeout if no data
    private static final int OSG_MAX_CONNECTIONS = 512; //Lots of connections since this is for the whole OSG

    private S3ClientOptions ops;
    private AmazonS3Client s3Client;

    public OsgInternalS3Client(AWSCredentials credentials, boolean https) {
        ClientConfiguration config = new ClientConfiguration();
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS); //very short timeout
        config.setSocketTimeout(OSG_SOCKET_TIMEOUT_MS);
        config.setUseReaper(true);
        config.setMaxConnections(OSG_MAX_CONNECTIONS);
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