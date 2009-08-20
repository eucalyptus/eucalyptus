/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.storage.fs.FileIO;

import java.io.IOException;
import java.util.List;

import org.jboss.netty.channel.Channel;

public interface StorageManager {

    public void checkPreconditions() throws EucalyptusCloudException;

    public boolean bucketExists(String bucket);
    
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

	public void sendObject(Channel channel, MappingHttpResponse httpResponse, String bucketName, String objectName, 
			long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed);

	public void sendObject(Channel channel, MappingHttpResponse httpResponse, String bucketName, String objectName, 
			long start, long end, long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed);

	public void sendHeaders(Channel channel, MappingHttpResponse httpResponse, Long size, String etag,
			String lastModified, String contentType, String contentDisposition);
	
    public void setRootDirectory(String rootDirectory);

    public void deleteSnapshot(String bucket, String snapshotId, String vgName, String lvName, List<String> snapshotSet, boolean removeVg) throws EucalyptusCloudException;

    public String createVolume(String bucket, List<String> snapshotSet, List<String> vgNames, List<String> lvNames, String snapshotId, String snapshotVgName, String snapshotLvName) throws EucalyptusCloudException;
}