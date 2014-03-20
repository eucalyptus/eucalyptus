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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotPart;
import com.eucalyptus.blockstorage.entities.SnapshotPart.SnapshotPartState;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo.SnapshotUploadState;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.exceptions.SnapshotFinalizeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotInitializeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotUploadException;
import com.eucalyptus.blockstorage.exceptions.SnapshotUploadPartException;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

/**
 * SnapshotUploader is meant for managing snapshot uploads to objectstorage. Caller must instantiate an object of this class using one of the constructors
 * before invoking any methods. Each object represents a specific snapshot and can be used to compress and upload the snapshot to objectstorage or cancel an
 * upload in progress
 * 
 * @author Swathi Gangisetty
 */
public class SnapshotUploader {

	private static Logger LOG = Logger.getLogger(SnapshotUploader.class);

	// Constructor parameters
	private String snapshotId;
	private String bucketName;
	private String keyName;
	private String fileName;
	private String uploadId;
	private AmazonS3 s3;

	// Instantiate from database or else where
	private Long partSize;
	private Integer queueSize;
	private Integer transferRetries;
	private Integer uploadTimeout;
	private ServiceConfiguration serviceConfig;

	private static Integer poolSize;

	private static final Integer READ_BUFFER_SIZE = 1024 * 1024;
	private static final Integer TX_RETRIES = 20;

	public SnapshotUploader() {
		StorageInfo info = StorageInfo.getStorageInfo();
		partSize = (long) (info.getSnapshotPartSizeInMB() * 1024 * 1024);
		queueSize = info.getMaxSnapshotPartsQueueSize();
		transferRetries = info.getMaxSnapTransferRetries();
		uploadTimeout = info.getSnapshotUploadTimeoutInHours();
		serviceConfig = Components.lookup(Storage.class).getLocalServiceConfiguration();
		if (poolSize == null) {
			poolSize = info.getMaxConcurrentSnapshotUploads();
		}
	}

	public SnapshotUploader(String snapshotId, String bucketName, String keyName, AmazonS3 s3) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.s3 = s3;
	}

	public SnapshotUploader(String snapshotId, String bucketName, String keyName, String fileName, AmazonS3 s3) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.fileName = fileName;
		this.s3 = s3;
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}

	public AmazonS3 getS3() {
		return s3;
	}

	public void setS3(AmazonS3 s3) {
		this.s3 = s3;
	}

	public Long getPartSize() {
		return partSize;
	}

	public void setPartSize(Long partSize) {
		this.partSize = partSize;
	}

	public Integer getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(Integer queueSize) {
		this.queueSize = queueSize;
	}

	public Integer getTransferRetries() {
		return transferRetries;
	}

	public void setTransferRetries(Integer transferRetries) {
		this.transferRetries = transferRetries;
	}

	public ServiceConfiguration getServiceConfig() {
		return serviceConfig;
	}

	public void setServiceConfig(ServiceConfiguration serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public void upload() throws SnapshotUploadException {
		verifyUploadParameters();

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
			// Setup the snapshot and part entities.
			snapUploadInfo = SnapshotUploadInfo.create(snapshotId, bucketName, keyName);
			Path zipFilePath = Files.createTempFile(keyName + '-', '-' + String.valueOf(partNumber));
			part = SnapshotPart.createPart(snapUploadInfo, zipFilePath.toString(), partNumber, readOffset);

			FileInputStream inputStream = new FileInputStream(fileName);
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
							uploadId = initiateMulitpartUpload();
							snapUploadInfo = snapUploadInfo.updateUploadId(uploadId);
							part = part.updateStateCreated(uploadId, bytesWritten, bytesRead, Boolean.FALSE);
							partQueue = new ArrayBlockingQueue<SnapshotPart>(queueSize);
							uploadPartsFuture = Threads.enqueue(serviceConfig, UploadPartTask.class, poolSize, new UploadPartTask(partQueue,
									new SnapshotProgressCallback(snapshotId, fileName)));
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
				throw new SnapshotUploadException("Failed to upload " + snapshotId + " due to: ", e);
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
					SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, bytesWritten);
					FileInputStreamWithCallback snapInputStream = new FileInputStreamWithCallback(new File(zipFilePath.toString()), callback);
					ObjectMetadata metadata = new ObjectMetadata();
					metadata.setContentLength(bytesWritten);
					PutObjectResult putResult = s3.putObject(bucketName, keyName, snapInputStream, metadata);
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
					throw new SnapshotUploadException("Failed to upload snapshot " + snapshotId + " due to: ", e);
				} finally {
					deleteFile(zipFilePath);
				}
			}
		} catch (SnapshotUploadException e) {
			error = Boolean.TRUE;
			throw e;
		} catch (Exception e) {
			error = Boolean.TRUE;
			LOG.error("Failed to upload snapshot " + snapshotId + " due to: ", e);
			throw new SnapshotUploadException("Failed to upload snapshot " + snapshotId + " due to: ", e);
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

	public void cancelUpload() throws SnapshotUploadException {
		verifyCancelUploadParameters();
		try (TransactionResource db = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(snapshotId, bucketName, keyName));
			uploadId = snapUploadInfo.getUploadId();
			abortMultipartUpload();
			snapUploadInfo.setState(SnapshotUploadState.aborted);
			db.commit();
		} catch (Exception e) {
			LOG.debug("Failed to cancel upload for snapshot " + snapshotId, e);
			throw new SnapshotUploadException("Failed to cancel upload for snapshot " + snapshotId, e);
		}
	}

	public void resumeUpload() throws EucalyptusCloudException {
		throw new EucalyptusCloudException("Not supported yet");
	}

	private void verifyUploadParameters() throws SnapshotUploadException {
		if (StringUtils.isBlank(snapshotId)) {
			throw new SnapshotUploadException("Snapshot ID is invalid. Cannot upload snapshot");
		}
		if (StringUtils.isBlank(bucketName)) {
			throw new SnapshotUploadException("Bucket name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (StringUtils.isBlank(keyName)) {
			throw new SnapshotUploadException("Key name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (StringUtils.isBlank(fileName)) {
			throw new SnapshotUploadException("Snapshot file name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (s3 == null) {
			throw new SnapshotUploadException("S3 client reference is invalid. Cannot upload snapshot " + snapshotId);
		}
	}

	private void verifyCancelUploadParameters() throws SnapshotUploadException {
		if (StringUtils.isBlank(snapshotId)) {
			throw new SnapshotUploadException("Snapshot ID is invalid. Cannot upload snapshot");
		}
		if (StringUtils.isBlank(bucketName)) {
			throw new SnapshotUploadException("Bucket name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (StringUtils.isBlank(keyName)) {
			throw new SnapshotUploadException("Key name is invalid. Cannot upload snapshot " + snapshotId);
		}
		if (s3 == null) {
			throw new SnapshotUploadException("S3 client reference is invalid. Cannot upload snapshot " + snapshotId);
		}
	}

	private String initiateMulitpartUpload() throws SnapshotInitializeMpuException {
		// Getting rid of retries for now as the client seems to retry
		InitiateMultipartUploadResult initResponse = null;
		try {
			LOG.info("Inititating multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName);
			initResponse = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, keyName));
		} catch (Exception ex) {
			throw new SnapshotInitializeMpuException("Failed to initialize multipart upload part for snapshotId=" + snapshotId + ", bucketName=" + bucketName
					+ ", keyName=" + keyName);
		}

		if (StringUtils.isBlank(initResponse.getUploadId())) {
			throw new SnapshotInitializeMpuException("Invalid upload ID for multipart upload part for snapshotId=" + snapshotId + ", bucketName=" + bucketName
					+ ", keyName=" + keyName);
		}
		return initResponse.getUploadId();
	}

	private PartETag uploadPart(SnapshotPart part, SnapshotProgressCallback progressCallback) throws SnapshotUploadPartException {
		// Getting rid of retries for now as the client seems to retry
		try {
			part = part.updateStateUploading();
		} catch (Exception e) {
			LOG.debug("Failed to update part status in DB. Moving on. " + part);
		}

		try {
			LOG.debug("Uploading " + part);
			UploadPartResult uploadPartResult = s3.uploadPart(new UploadPartRequest().withBucketName(part.getBucketName()).withKey(part.getKeyName())
					.withUploadId(part.getUploadId()).withPartNumber(part.getPartNumber()).withPartSize(part.getSize()).withFile(new File(part.getFileName())));
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
		// Getting rid of retries for now as the client seems to retry
		CompleteMultipartUploadResult result;
		try {
			LOG.info("Finalizing multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName + ", uploadId=" + uploadId);
			result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, keyName, uploadId, partETags));
		} catch (Exception ex) {
			LOG.debug("Failed to finalize multipart upload for snapshotId=" + snapshotId + ", bucketName=" + ", keyName=" + keyName, ex);
			throw new SnapshotFinalizeMpuException("Failed to initialize multipart upload part after for snapshotId=" + snapshotId + ", bucketName="
					+ bucketName + ", keyName=" + keyName);
		}

		return result.getETag();
	}

	private void abortMultipartUpload() {
		if (uploadId != null) {
			try {
				LOG.debug("Aborting multipart upload: snapshotId=" + snapshotId + ", bucketName=" + ", keyName=" + keyName + ", uploadId=" + uploadId);
				s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, keyName, uploadId));
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
			LOG.debug("Failed to delete file: " + fileName);
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
