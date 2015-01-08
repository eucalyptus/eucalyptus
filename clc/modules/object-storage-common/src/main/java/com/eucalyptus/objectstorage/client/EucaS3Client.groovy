/*
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 */

package com.eucalyptus.objectstorage.client

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.util.Md5Utils
import com.eucalyptus.auth.principal.Role
import com.eucalyptus.auth.principal.User
import com.eucalyptus.auth.tokens.SecurityToken
import com.eucalyptus.auth.tokens.SecurityTokenManager
import com.eucalyptus.util.EucalyptusCloudException
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.apache.xml.security.utils.Base64

import java.nio.charset.StandardCharsets

/**
 * This is how any internal eucalyptus component should get an s3 client and use it for object-storage access.
 */
@CompileStatic
class EucaS3ClientFactory {
    private static Logger LOG = Logger.getLogger(EucaS3ClientFactory.class);

    static boolean USE_HTTPS_DEFAULT = false;

    public static EucaS3Client getEucaS3Client(User clientUser, boolean useHttps) {
        return new EucaS3Client(clientUser, useHttps);
    }

    public static EucaS3Client getEucaS3Client(User clientUser) {
        return new EucaS3Client(clientUser, USE_HTTPS_DEFAULT);
    }

    public static EucaS3Client getEucaS3Client(AWSCredentials credentials) throws NoSuchElementException {
        return new EucaS3Client(credentials, USE_HTTPS_DEFAULT);
    }

    public static EucaS3Client getEucaS3Client(AWSCredentials credentials, boolean https) throws NoSuchElementException {
        return new EucaS3Client(credentials, https);
    }

    public static EucaS3Client getEucaS3Client(AWSCredentialsProvider credentials) throws NoSuchElementException {
        return new EucaS3Client(credentials, USE_HTTPS_DEFAULT);
    }

    public static EucaS3Client getEucaS3Client(AWSCredentialsProvider credentials, boolean https) throws NoSuchElementException {
        return new EucaS3Client(credentials, https);
    }

    public static EucaS3Client getEucaS3ClientByRole(Role role, int durationInSec) {

        EucaS3Client eucaS3Client;
        try {
            SecurityToken token = SecurityTokenManager.issueSecurityToken(role, durationInSec);
            eucaS3Client = EucaS3ClientFactory.getEucaS3Client(new BasicSessionCredentials(token.getAccessKeyId(), token.getSecretKey(), token.getToken()));
        } catch (Exception e) {
            LOG.error("Failed to initialize eucalyptus object storage client due to " + e);
            throw new EucalyptusCloudException("Failed to initialize eucalyptus object storage client", e);
        }
        return eucaS3Client;
    }

    public static EucaS3Client getEucaS3ClientForUser(User user, int durationInSec) {

        EucaS3Client eucaS3Client;
        try {
            SecurityToken token = SecurityTokenManager.issueSecurityToken(user, durationInSec);
            eucaS3Client = EucaS3ClientFactory.getEucaS3Client(new BasicSessionCredentials(token.getAccessKeyId(), token.getSecretKey(), token.getToken()));
        } catch (Exception e) {
            LOG.error("Failed to initialize eucalyptus object storage client due to " + e);
            throw new EucalyptusCloudException("Failed to initialize eucalyptus object storage client", e);
        }
        return eucaS3Client;
    }

}

/**
 * Wrapper class around an AmazonS3Client to provide some convenience functions for simple get/put of strings
 * Also provides methods for refreshing the endpoint in case of failure etc
 */
@CompileStatic
class EucaS3Client implements AmazonS3, AutoCloseable {

    @Delegate
    AmazonS3Client s3Client;

    protected EucaS3Client(User user, boolean useHttps) {
        this.s3Client = GenericS3ClientFactory.getS3ClientForUser(user, useHttps);
    }

    protected EucaS3Client(AWSCredentials credentials, boolean useHttps) {
        this.s3Client = GenericS3ClientFactory.getS3Client(credentials, useHttps);
    }

    protected EucaS3Client(AWSCredentialsProvider credentialsProvider, boolean useHttps) {
        this.s3Client = GenericS3ClientFactory.getS3Client(credentialsProvider, useHttps);
    }

    /**
     * Finds a new OSG to use for the endpoint. Use this method
     * in case of failure due to an OSG failing and becoming unavailable.
     */
    public void refreshEndpoint() throws NoSuchElementException {
        refreshEndpoint(false);
    }

    public void refreshEndpoint(boolean usePublicDns) throws NoSuchElementException {
        this.s3Client.setEndpoint(GenericS3ClientFactory.getRandomOSGUri(usePublicDns).toASCIIString());
    }

    public String getObjectContent( String bucket, String key, int maximumSize ) {
        final byte[] buffer = new byte[10*1024]; //10k buffer
        s3Client.getObject(bucket, key).getObjectContent( ).withStream{ S3ObjectInputStream contentStream ->
          final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream(buffer.length);
          int readBytes;
          while( (readBytes = contentStream.read( buffer ) ) > 0 ) {
            contentBytes.write( buffer, 0, readBytes );
            if ( contentBytes.size( ) > maximumSize ) {
              throw new IOException( "Maximum size exceeded for ${bucket}/${key}" )
            }
          }
          contentBytes.toString( StandardCharsets.UTF_8.name( ) );
        }
    }

    /**
     * Returns the etag, verifies content by md5
     * @param bucket
     * @param key
     * @param content
     * @return
     * @throws EucalyptusCloudException
     */
    public String putObjectContent(String bucket, String key, String content, Map<String, String> metadata) {
        byte[] contentBytes = content.getBytes( StandardCharsets.UTF_8 );
        byte[] md5 = Md5Utils.computeMD5Hash(contentBytes);
        ObjectMetadata objMetadata = new ObjectMetadata();
        if(metadata != null) {
            metadata.each { it ->
                Map.Entry<String,String> entry = ((Map.Entry<String,String>)it);
                objMetadata.addUserMetadata(entry.getKey(), entry.getValue());
            }
        }

        String base64Md5 = Base64.encode(md5);
        objMetadata.setContentMD5(base64Md5);
        ByteArrayInputStream contentStream = new ByteArrayInputStream(contentBytes);
        PutObjectResult result = s3Client.putObject(bucket, key, contentStream, objMetadata);
        return result.getETag();
    }

    public void close( ) {
      this.s3Client.shutdown( );
    }
}