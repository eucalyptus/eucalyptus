/*
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.util.Md5Utils
import com.eucalyptus.auth.principal.User
import com.eucalyptus.util.EucalyptusCloudException
import groovy.transform.CompileStatic
import org.apache.xml.security.utils.Base64

/**
 * This is how any internal eucalyptus component should get an s3 client and use it for object-storage access.
 */
@CompileStatic
class EucaS3ClientFactory {
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

}

/**
 * Wrapper class around an AmazonS3Client to provide some convenience functions for simple get/put of strings
 * Also provides methods for refreshing the endpoint in case of failure etc
 */
@CompileStatic
class EucaS3Client implements AmazonS3 {

    @Delegate
    AmazonS3Client s3Client;

    protected EucaS3Client(User user, boolean useHttps) {
        this.s3Client = GenericS3ClientFactory.getS3ClientForUser(user, useHttps);
    }

    protected EucaS3Client(AWSCredentials credentials, boolean useHttps) {
        this.s3Client = GenericS3ClientFactory.getS3Client(credentials, useHttps);
    }

    /**
     * Finds a new OSG to use for the endpoint. Use this method
     * in case of failure due to an OSG failing and becoming unavailable.
     */
    public void refreshEndpoint() throws NoSuchElementException {
        this.s3Client.setEndpoint(GenericS3ClientFactory.getRandomOSGUri().toASCIIString());
    }

    public String getObjectContent(String bucket, String key) {
        S3ObjectInputStream contentStream = null;
        byte[] buffer = new byte[10*1024]; //10k buffer
        int readBytes;
        ByteArrayOutputStream contentBytes = new ByteArrayOutputStream(buffer.length);
        try {
            S3Object manifest = s3Client.getObject(bucket, key);
            contentStream = manifest.getObjectContent();
            while((readBytes = contentStream.read(buffer)) > 0) {
                contentBytes.write(buffer, 0, readBytes);
            }
            return contentBytes.toString("UTF-8");
        } finally {
            if(contentStream != null) {
                contentStream.close();
            }
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
        byte[] contentBytes = content.getBytes("UTF-8");
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
}