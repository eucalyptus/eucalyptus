/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package edu.ucsb.eucalyptus.storage;

import java.io.IOException;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;

import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetRequestType;
import edu.ucsb.eucalyptus.storage.fs.FileIO;

public interface StorageManager {

    public void checkPreconditions() throws EucalyptusCloudException;

    public boolean bucketExists(String bucket);

    public boolean objectExists(String bucket, String object);
    
    public void createBucket(String bucket) throws IOException;

    public long getSize(String bucket, String object);

    public void deleteBucket(String bucket) throws IOException;

    public void createObject(String bucket, String object) throws IOException;

    public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException;

    public FileIO prepareForRead(String bucket, String object) throws Exception;

    public FileIO prepareForWrite(String bucket, String object) throws Exception;

    public int readObject(String bucket, String object, byte[] bytes, long offset) throws IOException;

    public int readObject(String objectPath, byte[] bytes, long offset) throws IOException;

    public void deleteObject(String bucket, String object) throws IOException;

    public void deleteAbsoluteObject(String object) throws IOException;

    public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException;

    public void renameObject(String bucket, String oldName, String newName) throws IOException;

    public String getObjectPath(String bucket, String object);

    public long getObjectSize(String bucket, String object);

    public void sendObject(WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, String bucketName, String objectName, 
			long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed, String versionId, BucketLogData logData);

    public void sendObject(WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, String bucketName, String objectName, 
			long start, long end, long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed, String versionId, BucketLogData logData);

    public void sendHeaders(WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, Long size, String etag,
			String lastModified, String contentType, String contentDisposition, String versionId, BucketLogData logData);
	
	public void disable() throws EucalyptusCloudException ;

	public void enable() throws EucalyptusCloudException;

	public void stop() throws EucalyptusCloudException;

	public void check() throws EucalyptusCloudException;

	public void start() throws EucalyptusCloudException;
}
