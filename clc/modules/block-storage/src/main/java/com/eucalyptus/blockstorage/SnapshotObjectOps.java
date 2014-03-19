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

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.util.S3Client;
import com.eucalyptus.tokens.CredentialsType;
import com.eucalyptus.util.EucalyptusCloudException;

public class SnapshotObjectOps {
	private Logger LOG = Logger.getLogger(SnapshotObjectOps.class);

	S3Client s3Client;

	public SnapshotObjectOps(CredentialsType credentials) {
		s3Client = new S3Client(new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken()),
				false);
		s3Client.setUsePathStyle(true);
		s3Client.setS3Endpoint(StorageProperties.WALRUS_URL);
		LOG.debug("Setting system property com.amazonaws.services.s3.disableGetObjectMD5Validation=true");
		System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", Boolean.TRUE.toString());
	}

	public void createBucket(String bucket) throws EucalyptusCloudException {
		LOG.debug("Creating objectstorage bucket " + bucket + " for storing snapshots");
		refreshOsgURI();
		try {
			s3Client.getS3Client().createBucket(bucket);
		} catch (Exception ex) {
			LOG.warn("Failed to create objectstorage bucket " + bucket, ex);
			throw new EucalyptusCloudException("Failed to create bucket: " + bucket, ex);
		}
	}

	public void uploadSnapshot(String snapshotId, String bucket, String key, String snapshotFileName) throws EucalyptusCloudException {
		refreshOsgURI();
		new SnapshotUploader(snapshotId, bucket, key, snapshotFileName, s3Client.getS3Client()).upload();
	}

	public void cancelSnapshotUpload(String snapshotId, String bucket, String key) throws EucalyptusCloudException {
		LOG.debug("Cancelling upload of snapshot " + snapshotId + " to objectstorage: bucket=" + bucket + ", key=" + key);
		refreshOsgURI();
		try {
			if (checkSnapshotExists(bucket, key)) {
				deleteSnapshot(bucket, key);
			} else {
				new SnapshotUploader(snapshotId, bucket, key, s3Client.getS3Client()).cancelUpload();
			}
		} catch (Exception ex) {
			LOG.warn("Failed to cancel of snapshot " + snapshotId + " to objectstorage: bucket=" + bucket + ", key=" + key, ex);
			throw new EucalyptusCloudException(ex);
		}
	}

	public void deleteSnapshot(String bucket, String key) throws EucalyptusCloudException {
		LOG.debug("Deleting snapshot from objectstorage: bucket=" + bucket + ", key=" + key);
		refreshOsgURI();
		try {
			s3Client.getS3Client().deleteObject(bucket, key);
		} catch (Exception ex) {
			LOG.warn("Failed to delete snapshot from objectstorage: bucket=" + bucket + ", key=" + key, ex);
			throw new EucalyptusCloudException(ex);
		}
	}

	public void downloadSnapshot(String bucket, String key, File snapshotFile) throws EucalyptusCloudException {
		LOG.debug("Downloading snapshot from objectstorage: bucket=" + bucket + ", key=" + key);
		refreshOsgURI();
		GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
		try {
			long startTime = System.currentTimeMillis();
			s3Client.getS3Client().getObject(getObjectRequest, snapshotFile);
			LOG.debug("Snapshot " + bucket + "/" + key + " download took " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
		} catch (Exception ex) {
			LOG.warn("Failed to downlaod snapshot from objectstorage: bucket=" + bucket + ", key=" + key, ex);
			throw new EucalyptusCloudException(ex);
		}
	}

	public long getSnapshotSize(String bucket, String key) throws EucalyptusCloudException {
		LOG.debug("Fetching snapshot size from objectstorage: bucket=" + bucket + ", key=" + key);
		refreshOsgURI();
		try {
			ObjectMetadata snapshotMetadata = s3Client.getS3Client().getObjectMetadata(bucket, key);
			return snapshotMetadata.getContentLength();
		} catch (Exception ex) {
			LOG.warn("Failed to fetch snapshot size from objectstorage: bucket=" + bucket + ", key=" + key, ex);
			throw new EucalyptusCloudException(ex);
		}
	}

	public Boolean checkSnapshotExists(String bucket, String key) throws EucalyptusCloudException {
		LOG.debug("Verifying if the snapshot is stored in objectstorage: bucket=" + bucket + ", key=" + key);
		refreshOsgURI();
		try {
			s3Client.getS3Client().getObjectMetadata(bucket, key);
			return Boolean.TRUE;
		} catch (AmazonServiceException aex) {
			// May be key off on error code for objectnotfound?
			return Boolean.FALSE;
		} catch (Exception ex) {
			LOG.warn("Failed to verify if the snapshot is stored in objectstorage: bucket=" + bucket + ", key=" + key, ex);
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
			LOG.debug("Setting objectstorage URI to: " + osgURI);
		} catch (Exception e) {
			LOG.warn("Could not refresh objectstorage URI");
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
		} catch (Exception e) {
			LOG.error("Could not obtain objectstorage information", e);
			throw e;
		}
	}
}
