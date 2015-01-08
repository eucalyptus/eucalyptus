/*************************************************************************
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotPart;
import com.eucalyptus.blockstorage.entities.SnapshotPart.SnapshotPartState;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo.SnapshotUploadState;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.exceptions.SnapshotFinalizeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotInitializeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotTransferException;
import com.eucalyptus.blockstorage.exceptions.SnapshotUploadPartException;
import com.eucalyptus.blockstorage.exceptions.UnknownFileSizeException;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

/**
 * S3SnapshotTransfer manages snapshot transfers between SC and S3 API such as objectstorage gateway. An instance of the class must be obtained using one of the
 * constructors before invoking any methods. It is recommended that every snapshot operation instantiate a new object of this class as the AmazonS3Client used
 * is not thread safe
 * 
 * @author Swathi Gangisetty
 */
public class S3SnapshotTransfer implements SnapshotTransfer {

	private static Logger LOG = Logger.getLogger(S3SnapshotTransfer.class);

	// Constructor parameters
	private String snapshotId;
	private String bucketName;
	private String keyName;

	// For multipart upload
	private String uploadId;

	// Initiate for every request
	private EucaS3Client eucaS3Client;

	// Instantiate from database for uploads
	private Long partSize;
	private Integer queueSize;
	private Integer transferRetries;
	private Integer uploadTimeout;
	private Integer poolSize;
	private ServiceConfiguration serviceConfig;

	// Static parameters
	private static Role role;

	// Constants
	private static final Integer READ_BUFFER_SIZE = 1024 * 1024;
	private static final Integer TX_RETRIES = 20;
	private static final Integer REFRESH_TOKEN_RETRIES = 1;
	private static final String UNCOMPRESSED_SIZE_KEY = "uncompressedsize";

	public S3SnapshotTransfer() throws SnapshotTransferException {
		initializeEucaS3Client();
	}

	public S3SnapshotTransfer(String snapshotId, String bucketName, String keyName) throws SnapshotTransferException {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
	}

	public S3SnapshotTransfer(String snapshotId, String keyName) throws SnapshotTransferException {
		this();
		this.snapshotId = snapshotId;
		this.keyName = keyName;
	}

	public String getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(String snapshotId) {
		this.snapshotId = snapshotId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}

	/**
	 * Preparation for upload involves looking up the bucket from the database and creating it in objectstorage gateway. If the bucket is already created,
	 * objectstorage gateway should still respond back with 200 OK. Invoke this method before uploading the snapshot using {@link #upload(String)} or set the
	 * bucket name explicitly using {@link #setBucketName(String)}
	 * 
	 * @return Name of the bucket that holds snapshots in objectstorage gateway
	 */
	@Override
	public String prepareForUpload() throws SnapshotTransferException {
		bucketName = createAndReturnBucketName();
		return bucketName;
	}

	/**
	 * Compresses the snapshot and uploads it to a bucket in objectstorage gateway as a single or multipart upload based on the configuration in
	 * {@link StorageInfo}. Bucket name should be configured before invoking this method. It can be looked up and initialized by {@link #prepareForUpload()} or
	 * explicitly set using {@link #setBucketName(String)}
	 * 
	 * @param sourceFileName
	 *            absolute path to the snapshot on the file system
	 */
	@Override
	public void upload(String sourceFileName) throws SnapshotTransferException {
		validateInput(); // Validate input
		loadTransferConfig(); // Load the transfer configuration parameters from database
		SnapshotProgressCallback progressCallback = new SnapshotProgressCallback(snapshotId); // Setup the progress callback

		Boolean error = Boolean.FALSE;
		ArrayBlockingQueue<SnapshotPart> partQueue = null;
		SnapshotPart part = null;
		SnapshotUploadInfo snapUploadInfo = null;
		Future<List<PartETag>> uploadPartsFuture = null;
		Future<String> completeUploadFuture = null;

		byte[] buffer = new byte[READ_BUFFER_SIZE];
		Long readOffset = 0L;
		Long bytesRead = 0L;
		Long bytesWritten = 0L;
		int len;
		int partNumber = 1;

		try {
			// Get the uncompressed file size for uploading as metadata
			Long uncompressedSize = getFileSize(sourceFileName);

			// Setup the snapshot and part entities.
			snapUploadInfo = SnapshotUploadInfo.create(snapshotId, bucketName, keyName);
			Path zipFilePath = Files.createTempFile(keyName + '-', '-' + String.valueOf(partNumber));
			part = SnapshotPart.createPart(snapUploadInfo, zipFilePath.toString(), partNumber, readOffset);

			FileInputStream inputStream = new FileInputStream(sourceFileName);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzipStream = new GZIPOutputStream(baos);
			FileOutputStream outputStream = new FileOutputStream(zipFilePath.toString());

			try {
				LOG.debug("Reading snapshot " + snapshotId + " and compressing it to disk in chunks of size " + partSize + " bytes or greater");
				while ((len = inputStream.read(buffer)) > 0) {
					bytesRead += len;
					gzipStream.write(buffer, 0, len);

					if ((bytesWritten + baos.size()) < partSize) {
						baos.writeTo(outputStream);
						bytesWritten += baos.size();
						baos.reset();
					} else {
						gzipStream.close();
						baos.writeTo(outputStream); // Order is important. Closing the gzip stream flushes stuff
						bytesWritten += baos.size();
						baos.reset();
						outputStream.close();

						if (partNumber > 1) {// Update the part status
							part = part.updateStateCreated(bytesWritten, bytesRead, Boolean.FALSE);
						} else {// Initialize multipart upload only once after the first part is created
							LOG.info("Uploading snapshot " + snapshotId + " to objectstorage using multipart upload");
							progressCallback.setUploadSize(uncompressedSize);
							uploadId = initiateMulitpartUpload(uncompressedSize);
							snapUploadInfo = snapUploadInfo.updateUploadId(uploadId);
							part = part.updateStateCreated(uploadId, bytesWritten, bytesRead, Boolean.FALSE);
							partQueue = new ArrayBlockingQueue<SnapshotPart>(queueSize);
							uploadPartsFuture = Threads.enqueue(serviceConfig, UploadPartTask.class, poolSize, new UploadPartTask(partQueue, progressCallback));
						}

						// Check for the future task before adding part to the queue.
						if (uploadPartsFuture != null && uploadPartsFuture.isDone()) {
							// This task shouldn't be done until the last part is added. If it is done at this point, then something might have gone wrong
							throw new SnapshotUploadPartException(
									"Error uploading parts, aborting part creation process. Check previous log messages for the exact error");
						}

						// Add part to the queue
						partQueue.put(part);

						// Prep the metadata for the next part
						readOffset += bytesRead;
						bytesRead = 0L;
						bytesWritten = 0L;

						// Setup the part entity for next part
						zipFilePath = Files.createTempFile(keyName + '-', '-' + String.valueOf((++partNumber)));
						part = SnapshotPart.createPart(snapUploadInfo, zipFilePath.toString(), partNumber, readOffset);

						gzipStream = new GZIPOutputStream(baos);
						outputStream = new FileOutputStream(zipFilePath.toString());
					}
				}

				gzipStream.close();
				baos.writeTo(outputStream);
				bytesWritten += baos.size();
				baos.reset();
				outputStream.close();
				inputStream.close();

				// Update the part status
				part = part.updateStateCreated(bytesWritten, bytesRead, Boolean.TRUE);

				// Update the snapshot upload info status
				snapUploadInfo = snapUploadInfo.updateStateCreatedParts(partNumber);
			} catch (Exception e) {
				LOG.error("Failed to upload " + snapshotId + " due to: ", e);
				error = Boolean.TRUE;
				throw new SnapshotTransferException("Failed to upload " + snapshotId + " due to: ", e);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
				if (gzipStream != null) {
					gzipStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
				baos.reset();
			}

			if (partNumber > 1) {
				// Check for the future task before adding the last part to the queue.
				if (uploadPartsFuture != null && uploadPartsFuture.isDone()) {
					// This task shouldn't be done until the last part is added. If it is done at this point, then something might have gone wrong
					throw new SnapshotUploadPartException(
							"Error uploading parts, aborting part upload process. Check previous log messages for the exact error");
				}
				// Add the last part to the queue
				partQueue.put(part);
				// Kick off the completion task
				completeUploadFuture = Threads.enqueue(serviceConfig, CompleteMpuTask.class, poolSize, new CompleteMpuTask(uploadPartsFuture, snapUploadInfo,
						partNumber));
			} else {
				try {
					LOG.info("Uploading snapshot " + snapshotId + " to objectstorage as a single object. Compressed size of snapshot (" + bytesWritten
							+ " bytes) is less than minimum part size (" + partSize + " bytes) for multipart upload");
					PutObjectResult putResult = uploadSnapshotAsSingleObject(zipFilePath.toString(), bytesWritten, uncompressedSize, progressCallback);
					markSnapshotAvailable();
					try {
						part = part.updateStateUploaded(putResult.getETag());
						snapUploadInfo = snapUploadInfo.updateStateUploaded(putResult.getETag());
					} catch (Exception e) {
						LOG.debug("Failed to update status in DB for " + snapUploadInfo);
					}
					LOG.info("Uploaded snapshot " + snapshotId + " to objectstorage");
				} catch (Exception e) {
					error = Boolean.TRUE;
					LOG.error("Failed to upload snapshot " + snapshotId + " due to: ", e);
					throw new SnapshotTransferException("Failed to upload snapshot " + snapshotId + " due to: ", e);
				} finally {
					deleteFile(zipFilePath);
				}
			}
		} catch (SnapshotTransferException e) {
			error = Boolean.TRUE;
			throw e;
		} catch (Exception e) {
			error = Boolean.TRUE;
			LOG.error("Failed to upload snapshot " + snapshotId + " due to: ", e);
			throw new SnapshotTransferException("Failed to upload snapshot " + snapshotId + " due to: ", e);
		} finally {
			if (error) {
				abortUpload(snapUploadInfo);
				if (uploadPartsFuture != null && !uploadPartsFuture.isDone()) {
					uploadPartsFuture.cancel(true);
				}
				if (completeUploadFuture != null && !completeUploadFuture.isDone()) {
					completeUploadFuture.cancel(true);
				}
			}
		}
	}

	/**
	 * Cancel the snapshot upload. Checks if a multipart upload is in progress and aborts the upload. Marks the upload as aborted for
	 * {@link SnapshotUploadCheckerTask} to clean up on its duty cycles
	 */
	@Override
	public void cancelUpload() throws SnapshotTransferException {
		validateInput();
		try (TransactionResource db = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(snapshotId, bucketName, keyName));
			uploadId = snapUploadInfo.getUploadId();
			abortMultipartUpload();
			snapUploadInfo.setState(SnapshotUploadState.aborted);
			db.commit();
		} catch (Exception e) {
			LOG.debug("Failed to cancel upload for snapshot " + snapshotId, e);
			throw new SnapshotTransferException("Failed to cancel upload for snapshot " + snapshotId, e);
		}
	}

	/**
	 * Not implemented
	 */
	@Override
	public void resumeUpload(String sourceFileName) throws SnapshotTransferException {
		throw new SnapshotTransferException("Not supported yet");
	}

	/**
	 * Downloads the compressed snapshot from objectstorage gateway to the filesystem
	 */
	@Override
	public void download(final String destinationFileName) throws SnapshotTransferException {
		LOG.debug("Downloading snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName + ", destinationFile="
				+ destinationFileName);
		validateInput();
		try {
			retryAfterRefresh(new Function<GetObjectRequest, ObjectMetadata>() {

				@Override
				@Nullable
				public ObjectMetadata apply(@Nullable GetObjectRequest arg0) {
					eucaS3Client.refreshEndpoint();
					return eucaS3Client.getObject(arg0, new File(destinationFileName));
				}

			}, new GetObjectRequest(bucketName, keyName), REFRESH_TOKEN_RETRIES);
		} catch (Exception e) {
			LOG.warn("Failed to download snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
			throw new SnapshotTransferException("Failed to download snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName
					+ ", key=" + keyName, e);
		}
	}

	/**
	 * Delete the snapshot from objectstorage gateway
	 */
	@Override
	public void delete() throws SnapshotTransferException {
		LOG.debug("Deleting snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
		validateInput();
		try {
			retryAfterRefresh(new Function<DeleteObjectRequest, String>() {

				@Override
				@Nullable
				public String apply(@Nullable DeleteObjectRequest arg0) {
					eucaS3Client.refreshEndpoint();
					eucaS3Client.deleteObject(arg0);
					return null;
				}
			}, new DeleteObjectRequest(bucketName, keyName), REFRESH_TOKEN_RETRIES);
		} catch (Exception e) {
			LOG.warn("Failed to delete snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
			throw new SnapshotTransferException("Failed to delete snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key="
					+ keyName, e);
		}
	}

	@Override
	public Long getSizeInBytes() {
		return null;
	}

	private void initializeEucaS3Client() throws SnapshotTransferException {
		if (role == null) {
			try {
				role = BlockStorageUtil.checkAndConfigureBlockStorageAccount();
			} catch (Exception e) {
				LOG.error("Failed to initialize account for snapshot transfers due to " + e);
				throw new SnapshotTransferException("Failed to initialize eucalyptus account for snapshot transfes", e);
			}
		}

		try {
			SecurityToken token = SecurityTokenManager.issueSecurityToken(role, (int) TimeUnit.HOURS.toSeconds(1));
			eucaS3Client = EucaS3ClientFactory.getEucaS3Client(new BasicSessionCredentials(token.getAccessKeyId(), token.getSecretKey(), token.getToken()));
		} catch (Exception e) {
			LOG.error("Failed to initialize S3 client for snapshot transfers due to " + e);
			throw new SnapshotTransferException("Failed to initialize S3 client for snapshot transfers", e);
		}
	}

	private void loadTransferConfig() {
		StorageInfo info = StorageInfo.getStorageInfo();
		this.partSize = (long) (info.getSnapshotPartSizeInMB() * 1024 * 1024);
		this.queueSize = info.getMaxSnapshotPartsQueueSize();
		this.transferRetries = info.getMaxSnapTransferRetries();
		this.uploadTimeout = info.getSnapshotUploadTimeoutInHours();
		this.serviceConfig = Components.lookup(Storage.class).getLocalServiceConfiguration();
		this.poolSize = info.getMaxConcurrentSnapshotUploads();
	}

	private void validateInput() throws SnapshotTransferException {
		if (StringUtils.isBlank(snapshotId)) {
			throw new SnapshotTransferException("Snapshot ID is invalid. Cannot upload snapshot");
		}
		if (StringUtils.isBlank(bucketName)) {
			throw new SnapshotTransferException("Bucket name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (StringUtils.isBlank(keyName)) {
			throw new SnapshotTransferException("Key name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (eucaS3Client == null) {
			throw new SnapshotTransferException("S3 client reference is invalid. Cannot upload snapshot " + snapshotId);
		}
	}

	private Long getFileSize(String fileName) throws UnknownFileSizeException {
		Long size = 0L;
		if ((size = new File(fileName).length()) <= 0) {
			try {
				CommandOutput result = SystemUtil.runWithRawOutput(new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "blockdev", "--getsize64", fileName });
				size = Long.parseLong(StringUtils.trimToEmpty(result.output));
			} catch (Exception ex) {
				throw new UnknownFileSizeException(fileName, ex);
			}
		}
		return size;
	}

	private String createAndReturnBucketName() throws SnapshotTransferException {
		String bucket = null;
		int bucketCreationRetries = SnapshotTransferConfiguration.DEFAULT_BUCKET_CREATION_RETRIES;
		do {
			bucketCreationRetries--;

			// Get the snapshot bucket name
			if (StringUtils.isBlank(bucket)) {
				try {
					// Get the snapshot configuration
					bucket = SnapshotTransferConfiguration.getInstance().getSnapshotBucket();
				} catch (Exception ex1) {
					try {
						// It might not exist, create one
						bucket = SnapshotTransferConfiguration.updateBucketName(
								StorageProperties.SNAPSHOT_BUCKET_PREFIX + UUID.randomUUID().toString().replaceAll("-", "")).getSnapshotBucket();
					} catch (Exception ex2) {
						// Chuck it, just make up a bucket name and go with it. Bucket location gets persisted in the snapshotinfo entity anyways
						bucket = StorageProperties.SNAPSHOT_BUCKET_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
					}
				}
			}

			// Try creating the bucket
			try {
				retryAfterRefresh(new Function<String, Bucket>() {

					@Override
					@Nullable
					public Bucket apply(@Nullable String arg0) {
						eucaS3Client.refreshEndpoint();
						return eucaS3Client.createBucket(arg0);
					}
				}, bucket, REFRESH_TOKEN_RETRIES);
				break;
			} catch (Exception ex) {
				// If bucket creation fails, try using a different bucket name
				if (bucketCreationRetries > 0) {
					LOG.debug("Unable to create snapshot upload bucket " + bucket + ". Will retry with a different bucket name");
					try {
						bucket = SnapshotTransferConfiguration.updateBucketName(
								StorageProperties.SNAPSHOT_BUCKET_PREFIX + UUID.randomUUID().toString().replaceAll("-", "")).getSnapshotBucket();
					} catch (Exception ex2) {
						// Chuck it, just make up a bucket name and go with it. Bucket location gets persisted in the snapshotinfo entity anyways
						bucket = StorageProperties.SNAPSHOT_BUCKET_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
					}
				} else {
					throw new SnapshotTransferException("Unable to create bucket for snapshot uploads after "
							+ SnapshotTransferConfiguration.DEFAULT_BUCKET_CREATION_RETRIES + " retries");
				}
			}
		} while (bucketCreationRetries > 0);

		return bucket;
	}

	private PutObjectResult uploadSnapshotAsSingleObject(String compressedSnapFileName, Long actualSize, Long uncompressedSize,
			SnapshotProgressCallback callback) throws Exception {
		callback.setUploadSize(actualSize);
		FileInputStreamWithCallback snapInputStream = new FileInputStreamWithCallback(new File(compressedSnapFileName), callback);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		Map<String, String> userMetadataMap = new HashMap<String, String>();
		userMetadataMap.put(UNCOMPRESSED_SIZE_KEY, String.valueOf(uncompressedSize)); // Send the uncompressed length as the metadata
		objectMetadata.setUserMetadata(userMetadataMap);
		objectMetadata.setContentLength(actualSize);

		return retryAfterRefresh(new Function<PutObjectRequest, PutObjectResult>() {

			@Override
			@Nullable
			public PutObjectResult apply(@Nullable PutObjectRequest arg0) {
				eucaS3Client.refreshEndpoint();
				return eucaS3Client.putObject(arg0);
			}

		}, new PutObjectRequest(bucketName, keyName, snapInputStream, objectMetadata), REFRESH_TOKEN_RETRIES);
	}

	private String initiateMulitpartUpload(Long uncompressedSize) throws SnapshotInitializeMpuException {
		InitiateMultipartUploadResult initResponse = null;
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		Map<String, String> userMetadataMap = new HashMap<String, String>();
		userMetadataMap.put(UNCOMPRESSED_SIZE_KEY, String.valueOf(uncompressedSize)); // Send the uncompressed length as the metadata
		objectMetadata.setUserMetadata(userMetadataMap);
		initRequest.setObjectMetadata(objectMetadata);

		try {
			LOG.info("Inititating multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName);
			initResponse = retryAfterRefresh(new Function<InitiateMultipartUploadRequest, InitiateMultipartUploadResult>() {

				@Override
				@Nullable
				public InitiateMultipartUploadResult apply(@Nullable InitiateMultipartUploadRequest arg0) {
					eucaS3Client.refreshEndpoint();
					return eucaS3Client.initiateMultipartUpload(arg0);
				}

			}, initRequest, REFRESH_TOKEN_RETRIES);
		} catch (Exception ex) {
			throw new SnapshotInitializeMpuException("Failed to initialize multipart upload part for snapshotId=" + snapshotId + ", bucketName=" + bucketName
					+ ", keyName=" + keyName, ex);
		}

		if (StringUtils.isBlank(initResponse.getUploadId())) {
			throw new SnapshotInitializeMpuException("Invalid upload ID for multipart upload part for snapshotId=" + snapshotId + ", bucketName=" + bucketName
					+ ", keyName=" + keyName);
		}
		return initResponse.getUploadId();
	}

	private PartETag uploadPart(SnapshotPart part, SnapshotProgressCallback progressCallback) throws SnapshotUploadPartException {
		try {
			part = part.updateStateUploading();
		} catch (Exception e) {
			LOG.debug("Failed to update part status in DB. Moving on. " + part);
		}

		try {
			LOG.debug("Uploading " + part);
			UploadPartResult uploadPartResult = retryAfterRefresh(
					new Function<UploadPartRequest, UploadPartResult>() {

						@Override
						@Nullable
						public UploadPartResult apply(@Nullable UploadPartRequest arg0) {
							eucaS3Client.refreshEndpoint();
							return eucaS3Client.uploadPart(arg0);
						}
					},
					new UploadPartRequest().withBucketName(part.getBucketName()).withKey(part.getKeyName()).withUploadId(part.getUploadId())
							.withPartNumber(part.getPartNumber()).withPartSize(part.getSize()).withFile(new File(part.getFileName())), REFRESH_TOKEN_RETRIES);

			progressCallback.update(part.getInputFileBytesRead());

			try {
				part = part.updateStateUploaded(uploadPartResult.getPartETag().getETag());
			} catch (Exception e) {
				LOG.debug("Failed to update part status in DB. Moving on. " + part);
			}
			LOG.debug("Uploaded " + part);
			return uploadPartResult.getPartETag();
		} catch (Exception e) {
			LOG.error("Failed to upload part " + part, e);
			try {
				part = part.updateStateFailed();
			} catch (Exception ie) {
				LOG.debug("Failed to update part status in DB. Moving on. " + part);
			}
			throw new SnapshotUploadPartException("Failed to upload part " + part, e);
		} finally {
			deleteFile(part.getFileName());
		}
	}

	private String finalizeMultipartUpload(List<PartETag> partETags) throws SnapshotFinalizeMpuException {
		CompleteMultipartUploadResult result;
		try {
			LOG.info("Finalizing multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName + ", uploadId=" + uploadId);
			result = retryAfterRefresh(new Function<CompleteMultipartUploadRequest, CompleteMultipartUploadResult>() {

				@Override
				@Nullable
				public CompleteMultipartUploadResult apply(@Nullable CompleteMultipartUploadRequest arg0) {
					eucaS3Client.refreshEndpoint();
					return eucaS3Client.completeMultipartUpload(arg0);
				}
			}, new CompleteMultipartUploadRequest(bucketName, keyName, uploadId, partETags), REFRESH_TOKEN_RETRIES);
			return result.getETag();
		} catch (Exception ex) {
			LOG.debug("Failed to finalize multipart upload for snapshotId=" + snapshotId + ", bucketName=" + ", keyName=" + keyName, ex);
			throw new SnapshotFinalizeMpuException("Failed to initialize multipart upload part after for snapshotId=" + snapshotId + ", bucketName="
					+ bucketName + ", keyName=" + keyName);
		}
	}

	private void abortMultipartUpload() {
		if (uploadId != null) {
			try {
				LOG.debug("Aborting multipart upload: snapshotId=" + snapshotId + ", bucketName=" + ", keyName=" + keyName + ", uploadId=" + uploadId);
				retryAfterRefresh(new Function<AbortMultipartUploadRequest, String>() {

					@Override
					@Nullable
					public String apply(@Nullable AbortMultipartUploadRequest arg0) {
						eucaS3Client.refreshEndpoint();
						eucaS3Client.abortMultipartUpload(arg0);
						return null;
					}
				}, new AbortMultipartUploadRequest(bucketName, keyName, uploadId), REFRESH_TOKEN_RETRIES);
			} catch (Exception e) {
				LOG.debug("Failed to abort multipart upload for snapshot " + snapshotId);
			}
		}
	}

	private void abortUpload(SnapshotUploadInfo snapUploadInfo) {
		abortMultipartUpload();
		if (snapUploadInfo != null) {
			try {
				snapUploadInfo.updateStateAborted();
			} catch (EucalyptusCloudException e) {
				LOG.debug("Failed to update status in DB for " + snapUploadInfo);
			}
		}
	}

	private void deleteFile(String fileName) {
		if (StringUtils.isNotBlank(fileName)) {
			try {
				Files.deleteIfExists(Paths.get(fileName));
			} catch (IOException e) {
				LOG.debug("Failed to delete file: " + fileName);
			}
		}
	}

	private void deleteFile(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			LOG.debug("Failed to delete file: " + path.toString());
		}
	}

	private void markSnapshotAvailable() throws TransactionException, NoSuchElementException {
		Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {

			@Override
			public SnapshotInfo apply(String arg0) {
				SnapshotInfo snap;
				try {
					snap = Entities.uniqueResult(new SnapshotInfo(arg0));
					snap.setStatus(StorageProperties.Status.available.toString());
					snap.setProgress("100");
					snap.setSnapPointId(null);
					return snap;
				} catch (TransactionException | NoSuchElementException e) {
					LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
				}
				return null;
			}
		};

		Entities.asTransaction(SnapshotInfo.class, updateFunction, TX_RETRIES).apply(snapshotId);
	}

	private void markSnapshotFailed() throws TransactionException, NoSuchElementException {
		Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {

			@Override
			public SnapshotInfo apply(String arg0) {
				SnapshotInfo snap;
				try {
					snap = Entities.uniqueResult(new SnapshotInfo(arg0));
					snap.setStatus(StorageProperties.Status.failed.toString());
					snap.setProgress("0");
					return snap;
				} catch (TransactionException | NoSuchElementException e) {
					LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
				}
				return null;
			}
		};

		Entities.asTransaction(SnapshotInfo.class, updateFunction, TX_RETRIES).apply(snapshotId);
	}

	private <F, T> T retryAfterRefresh(Function<F, T> function, F input, int retries) throws SnapshotTransferException {
		int failedAttempts = 0;
		T output = null;
		do {
			try {
				output = function.apply(input);
				break;
			} catch (AmazonServiceException e) {
				if (failedAttempts < retries && e.getStatusCode() == HttpResponseStatus.FORBIDDEN.getCode()) {
					LOG.debug("Snapshot transfer operation failed because of " + e.getMessage() + ". Will refresh credentials and retry");
					failedAttempts++;
					initializeEucaS3Client();
					continue;
				} else {
					throw new SnapshotTransferException("Snapshot transfer operation failed because of", e);
				}
			} catch (Exception e) {
				throw new SnapshotTransferException("Snapshot transfer operation failed because of", e);
			}
		} while (failedAttempts <= retries);

		return output;
	}

	class UploadPartTask implements Callable<List<PartETag>> {

		private ArrayBlockingQueue<SnapshotPart> partQueue;
		private SnapshotProgressCallback progressCallback;
		private List<PartETag> partETags;

		public UploadPartTask(ArrayBlockingQueue<SnapshotPart> partQueue, SnapshotProgressCallback progressCallback) throws EucalyptusCloudException {
			if (partQueue == null || progressCallback == null) {
				throw new EucalyptusCloudException("Invalid constructor parameters. Cannot proceed without part queue and or snapshot progress callback");
			}
			this.partQueue = partQueue;
			this.progressCallback = progressCallback;
			this.partETags = new ArrayList<PartETag>();
		}

		@Override
		public List<PartETag> call() throws Exception {
			Boolean isLast = Boolean.FALSE;
			do {
				SnapshotPart part = null;

				try {
					part = partQueue.take();
				} catch (InterruptedException ex) { // Should rarely happen
					LOG.error("Failed to upload snapshot " + snapshotId + " due to an retrieving parts from queue", ex);
					return null;
				}

				isLast = part.getIsLast();

				if (part.getState().equals(SnapshotPartState.created) || part.getState().equals(SnapshotPartState.uploading)
						|| part.getState().equals(SnapshotPartState.failed)) {
					try {
						partETags.add(uploadPart(part, progressCallback));
					} catch (Exception e) {
						LOG.error("Failed to upload a part for " + snapshotId + ". Aborting the part upload process");
						return null;
					}
				} else {
					LOG.warn("Not sure what to do with this part, just keep going: " + part);
				}
			} while (!isLast);

			return partETags;
		}
	}

	class CompleteMpuTask implements Callable<String> {

		private Future<List<PartETag>> uploadTaskFuture;
		private SnapshotUploadInfo snapUploadInfo;
		private Integer totalParts;

		public CompleteMpuTask(Future<List<PartETag>> uploadTaskFuture, SnapshotUploadInfo snapUploadInfo, Integer totalParts) {
			this.uploadTaskFuture = uploadTaskFuture;
			this.snapUploadInfo = snapUploadInfo;
			this.totalParts = totalParts;
		}

		@Override
		public String call() throws Exception {
			Boolean error = Boolean.FALSE;
			String etag = null;
			try {
				List<PartETag> partETags = uploadTaskFuture.get(uploadTimeout, TimeUnit.HOURS);
				if (partETags != null && partETags.size() == totalParts) {
					try {
						etag = finalizeMultipartUpload(partETags);
						markSnapshotAvailable();
						try {
							snapUploadInfo = snapUploadInfo.updateStateUploaded(etag);
						} catch (Exception e) {
							LOG.debug("Failed to update status in DB for " + snapUploadInfo);
						}
						LOG.info("Uploaded snapshot " + snapUploadInfo.getSnapshotId() + " to objectstorage");
					} catch (Exception e) {
						error = Boolean.TRUE;
						LOG.error("Failed to upload " + snapshotId + " due to an error completing the upload", e);
					}
				} else {
					error = Boolean.TRUE;
					LOG.error("Failed to upload " + snapshotId + " as the total number of parts does not tally up against the part Etags");
				}
			} catch (TimeoutException tex) {
				error = Boolean.TRUE;
				LOG.error("Failed to upload " + snapshotId + ". Complete upload task timed out waiting on upload part task after " + uploadTimeout + " hours");
			} catch (Exception ex) {
				error = Boolean.TRUE;
				LOG.error("Failed to upload " + snapshotId, ex);
			} finally {
				if (error) {
					markSnapshotFailed();
					abortUpload(snapUploadInfo);
				}
			}
			return etag;
		}
	}
}
