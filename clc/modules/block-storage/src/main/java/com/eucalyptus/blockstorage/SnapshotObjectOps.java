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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.eucalyptus.blockstorage.exceptions.UnknownFileSizeException;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.util.S3Client;
import com.eucalyptus.tokens.CredentialsType;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;


public class SnapshotObjectOps {
    private Logger LOG = Logger.getLogger( SnapshotObjectOps.class );

    S3Client s3Client;

    public SnapshotObjectOps(CredentialsType credentials) {
            s3Client = new S3Client(new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken()), false);
            s3Client.setUsePathStyle(true);
            s3Client.setS3Endpoint(StorageProperties.WALRUS_URL);
    }
    
    // TODO EUCA-8700 - Temporary workaround for snapshot uploads to work. THIS CANNOT BE RELEASED
    public SnapshotObjectOps(String accessKey, String secretKey) {
    	s3Client = new S3Client(new BasicAWSCredentials(accessKey, secretKey), false);
    	s3Client.setUsePathStyle(true);
    	s3Client.setS3Endpoint(StorageProperties.WALRUS_URL);
	}

    public void uploadSnapshot(File snapshotFile,
                               SnapshotProgressCallback callback,
                               String snapshotKey,
                               String snapshotId) throws EucalyptusCloudException {
    	refreshOsgURI();
        try {
            s3Client.getS3Client().listObjects(StorageProperties.SNAPSHOT_BUCKET);
        } catch (Exception ex) {
            try {
                //if (!s3Client.getS3Client().doesBucketExist(StorageProperties.SNAPSHOT_BUCKET)) {
                s3Client.getS3Client().createBucket(StorageProperties.SNAPSHOT_BUCKET);
                //}
            } catch (Exception e) {
                LOG.error("Snapshot upload failed. Unable to create bucket: snapshots", e);
                throw new EucalyptusCloudException(e);
            }
        }
        try {
        	FileInputStreamWithCallback snapInputStream = new FileInputStreamWithCallback(snapshotFile, callback);
        	ObjectMetadata metadata = new ObjectMetadata();
        	metadata.setContentLength(snapInputStream.getFileSize());
            //FIXME: need to set MD5
            //use multipart upload if requested
            //seek and compress parts
            //use a thread pool to get segments, compress and perform upload parts...ideally we want to use an existing one
            //when all done, raise flag and complete upload or abort if a part failed
            s3Client.getS3Client().putObject(StorageProperties.SNAPSHOT_BUCKET, snapshotKey, snapInputStream, metadata);
        } catch (Exception ex) {
            LOG.error("Snapshot " + snapshotId + " upload failed to: " + snapshotKey, ex);
            throw new EucalyptusCloudException(ex);
        }
    }

    public void deleteSnapshot(String snapshotLocation, String snapshotId) throws EucalyptusCloudException {
    	refreshOsgURI();
        try {
            s3Client.getS3Client().deleteObject(StorageProperties.SNAPSHOT_BUCKET, snapshotLocation);
        } catch (Exception ex) {
            LOG.error("Snapshot delete failed for: " + snapshotId, ex);
            throw new EucalyptusCloudException(ex);
        }
        /*try {
            s3Client.getS3Client().deleteBucket(StorageProperties.SNAPSHOT_BUCKET);
        } catch (Exception ex) {
            LOG.debug("Snapshot bucket delete failed for: " + StorageProperties.SNAPSHOT_BUCKET, ex);
        }*/
    }

    public void downloadSnapshot(String snapshotBucket, String snapshotLocation,
                                 File tmpCompressedFile) throws EucalyptusCloudException {
    	refreshOsgURI();
        GetObjectRequest getObjectRequest = new GetObjectRequest(snapshotBucket, snapshotLocation);
        try {
            long startTime = System.currentTimeMillis();
            s3Client.getS3Client().getObject(getObjectRequest, tmpCompressedFile);
            LOG.info("Snapshot " + snapshotBucket + "/" + snapshotLocation + " download took " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception ex) {
            LOG.error("Snapshot download failed for: " + snapshotLocation, ex);
            throw new EucalyptusCloudException(ex);
        }
    }
    
    /**
	 * Utility method for refreshing OSG URI in the S3Client object used by this class.
	 */
	private void refreshOsgURI() {
		try {
			ServiceConfiguration osgConfig = getOsgConfig();
			String osgURI = ServiceUris.remote(osgConfig).toASCIIString();
			s3Client.setS3Endpoint(osgURI);
			LOG.info("Setting OSG URI to: " + osgURI);
		}  catch (Exception e) {
			LOG.warn("Could not refresh OSG URI");
		}
	}
	
	/**
	 * Utility method for obtaining the service configuration of OSG. This could probably move up into some util class later
	 * 
	 */
	private ServiceConfiguration getOsgConfig() {
		try {
			// TODO Add/modify method to look up all enabled OSGs and pick one at random. 
			ServiceConfiguration osgConfig = Topology.lookup(ObjectStorage.class);
			return osgConfig;
		}  catch (Exception e) {
			LOG.error("Could not obtain OSG information", e);
			throw e;
		}
	}
	
	 /**
     * File stream with update callbacks to update on progress. Will call
     * callback on each operation, callback must be selective on when to do update and
     * should do so asynchronously for best performance.
     *
     */
    public class FileInputStreamWithCallback extends FileInputStream {

        private long totalRead;
        private long fileSize;
        private SnapshotProgressCallback callback;

        public FileInputStreamWithCallback(File file, SnapshotProgressCallback callback)
                throws FileNotFoundException, UnknownFileSizeException {
            super(file);
            totalRead = 0;
            if (file.length() > 0) {
				fileSize = file.length();
			} else {
				try {
					CommandOutput result = SystemUtil.runWithRawOutput(new String[] { StorageProperties.EUCA_ROOT_WRAPPER,
							"blockdev", "--getsize64", file.getAbsolutePath() });
					fileSize = Long.parseLong(StringUtils.trimToEmpty(result.output));
				} catch (Exception ex) {
					throw new UnknownFileSizeException(file.getAbsolutePath(), ex);
				}
			}
            this.callback = callback;
        }
        
        public long getFileSize() {
			return fileSize;
		}

        @Override
        public int available() throws IOException {
            int available = super.available();
            if (available == 0) {
                callback.finish();
            }
            return available;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            totalRead += bytesRead;
            callback.update(totalRead);
            return bytesRead;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int bytesRead = super.read(b);
            totalRead += bytesRead;
            callback.update(totalRead);
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            if(totalRead == fileSize) {
                callback.finish();
            } else {
                callback.failed();
            }
            super.close();
        }

    }
}
