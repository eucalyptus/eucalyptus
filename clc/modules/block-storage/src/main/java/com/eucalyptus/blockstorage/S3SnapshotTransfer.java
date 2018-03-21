/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.blockstorage.async.SnapshotTransferCleaner;
import com.eucalyptus.blockstorage.entities.SnapshotPart;
import com.eucalyptus.blockstorage.entities.SnapshotPart.SnapshotPartState;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo.SnapshotUploadState;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.exceptions.SnapshotFinalizeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotInitializeMpuException;
import com.eucalyptus.blockstorage.exceptions.SnapshotTransferException;
import com.eucalyptus.blockstorage.exceptions.SnapshotUploadObjectException;
import com.eucalyptus.blockstorage.exceptions.SnapshotUploadPartException;
import com.eucalyptus.blockstorage.threadpool.SnapshotTransferThreadPool;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;

/**
 * S3SnapshotTransfer manages snapshot transfers between SC and S3 API such as objectstorage gateway. An instance of the class must be obtained using
 * one of the constructors before invoking any methods. It is recommended that every snapshot operation instantiate a new object of this class as the
 * AmazonS3Client used is not thread safe
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
  private Integer transferTimeout;
  private Integer readBufferSize;
  private Integer writeBufferSize;

  // Static parameters
  private static BaseRole role;

  // Constants
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

  // for using in unit tests
  protected S3SnapshotTransfer(boolean mock) {
    // for mocking, do not initialize s3 client
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
   * Preparation for upload involves looking up the bucket from the database and creating it in objectstorage gateway. If the bucket is already
   * created, objectstorage gateway should still respond back with 200 OK. Invoke this method before uploading the snapshot using
   * {@link #upload(String, SnapshotProgressCallback)} or set the bucket name explicitly using {@link #setBucketName(String)}
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
   * {@link StorageInfo}. Bucket name should be configured before invoking this method. It can be looked up and initialized by
   * {@link #prepareForUpload()} or explicitly set using {@link #setBucketName(String)}
   * 
   * @param sourceFileName absolute path to the snapshot on the file system
   */
  @Override
  public Future<String> upload(StorageResource storageResource, SnapshotProgressCallback progressCallback) throws SnapshotTransferException {
    validateInput(); // Validate input
    loadTransferConfig(); // Load the transfer configuration parameters from database

    Boolean error = Boolean.FALSE;
    ArrayBlockingQueue<SnapshotPart> partQueue = null;
    SnapshotPart part = null;
    SnapshotUploadInfo snapUploadInfo = null;
    Future<List<PartETag>> uploadPartsFuture = null;
    Future<String> completeUploadFuture = null;

    byte[] buffer = new byte[readBufferSize];
    Long readOffset = 0L;
    Long bytesRead = 0L;
    Long bytesWritten = 0L;
    int len;
    int partNumber = 1;

    try {
      // Get the uncompressed file size for uploading as metadata
      Long uncompressedSize = storageResource.getSize();
      LOG.debug("Uncompressed size of content to be uploaded for " + snapshotId + ": " + uncompressedSize + " bytes");

      // Setup the snapshot and part entities.
      snapUploadInfo = SnapshotUploadInfo.create(snapshotId, bucketName, keyName);
      Path zipFilePath = Files.createTempFile(Paths.get("/var/tmp"), keyName + '-', '-' + String.valueOf(partNumber));
      part = SnapshotPart.createPart(snapUploadInfo, zipFilePath.toString(), partNumber, readOffset);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzipStream = new GZIPOutputStream(baos);
      FileOutputStream outputStream = new FileOutputStream(zipFilePath.toString());
      InputStream inputStream = null;

      try {
        LOG.debug("Reading snapshot " + snapshotId + " and compressing it to disk in chunks of size " + partSize + " bytes or greater");
        inputStream = storageResource.getInputStream( );
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
              snapUploadInfo = snapUploadInfo.updateUploadId(uploadId); // update uploadId so its available for future parts
              part = part.updateStateCreated(uploadId, bytesWritten, bytesRead, Boolean.FALSE);
              partQueue = new ArrayBlockingQueue<SnapshotPart>(queueSize);
              uploadPartsFuture = SnapshotTransferThreadPool.add(new UploadPartTask(partQueue, progressCallback));
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
            zipFilePath = Files.createTempFile(Paths.get("/var/tmp"), keyName + '-', '-' + String.valueOf((++partNumber)));
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
          try {
            inputStream.close();
          } catch (Exception e) {

          }
        }
        if (gzipStream != null) {
          try {
            gzipStream.close();
          } catch (Exception e) {

          }
        }
        if (outputStream != null) {
          try {
            outputStream.close();
          } catch (Exception e) {

          }
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
        completeUploadFuture = SnapshotTransferThreadPool.add(new CompleteMpuTask(uploadPartsFuture, snapUploadInfo, partNumber));
      } else {
        try {
          LOG.info("Uploading snapshot " + snapshotId + " to objectstorage as a single object. Compressed size of snapshot (" + bytesWritten
              + " bytes) is less than minimum part size (" + partSize + " bytes) for multipart upload");
          completeUploadFuture =
              SnapshotTransferThreadPool.add(new UploadObjectTask(part, snapUploadInfo, zipFilePath.toString(), bytesWritten, uncompressedSize,
                  progressCallback));
        } catch (Exception e) {
          error = Boolean.TRUE;
          LOG.error("Failed to add async task for uploading " + snapshotId + " due to: ", e);
          throw new SnapshotUploadObjectException("Failed to add async task for uploading " + snapshotId + " due to: ", e);
        }
      }
      return completeUploadFuture;
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
        if (partQueue != null) {
          partQueue.clear();
        }
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
   * {@link SnapshotTransferCleaner} to clean up on its duty cycles
   */
  @Override
  public void cancelUpload() throws SnapshotTransferException {
    validateInput();
    try (TransactionResource db = Entities.transactionFor(SnapshotUploadInfo.class)) {
      SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(snapshotId, bucketName, keyName));
      uploadId = snapUploadInfo.getUploadId();
      if (!snapUploadInfo.getState().equals(SnapshotUploadState.aborted)) {
        abortMultipartUpload();
        snapUploadInfo.setState(SnapshotUploadState.aborted);
      }
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
  public void resumeUpload(StorageResource storageResource) throws SnapshotTransferException {
    throw new SnapshotTransferException("Not supported yet");
  }

  /**
   * Downloads the compressed snapshot from objectstorage gateway to the filesystem
   */
  public void download(StorageResource storageResource) throws SnapshotTransferException {
    validateInput();
    loadTransferConfig();

    S3Object snapObj = download();

    if (snapObj != null && snapObj.getObjectContent() != null) {
      byte[] buffer = new byte[10 * readBufferSize];
      int len;
      GZIPInputStream gzipInputStream = null;

      try {
        gzipInputStream = new GZIPInputStream(new BufferedInputStream(snapObj.getObjectContent(), buffer.length * 3), buffer.length * 2);

        if (storageResource.isDownloadSynchronous()) { // Download and unzip snapshot to the storage device directly
          OutputStream outputStream = null;
          try {
            outputStream = storageResource.getOutputStream();
            while ((len = gzipInputStream.read(buffer)) > 0) {
              // Write to the output stream
              outputStream.write(buffer, 0, len);
            }

            // Close the streams and free the resources
            gzipInputStream.close();
            outputStream.close();
            buffer = null;
          } finally {
            try {
              if (outputStream != null)
                outputStream.close();
            } catch (Exception e) {

            }
          }
        } else { // Download and unzip snpashot to disk in parts and write the parts to storage backend in parallel
          ArrayBlockingQueue<SnapshotPart> partQueue = new ArrayBlockingQueue<SnapshotPart>(queueSize);
          Future<String> storageWriterFuture = SnapshotTransferThreadPool.add(new StorageWriterTask(partQueue, storageResource));

          FileOutputStream fileOutputStream = null;
          long bytesWritten = 0;
          int partNumber = 1;

          try {
            Path filePath = Files.createTempFile(Paths.get("/var/tmp"), snapshotId + '-', '-' + String.valueOf(partNumber));
            fileOutputStream = new FileOutputStream(filePath.toString());
            SnapshotPart part = new SnapshotPart();
            part.setFileName(filePath.toString());
            part.setPartNumber(partNumber);
            part.setIsLast(Boolean.FALSE);

            while ((len = gzipInputStream.read(buffer)) > 0) {
              if ((bytesWritten + len) < writeBufferSize) {
                fileOutputStream.write(buffer, 0, len);
                bytesWritten += len;
              } else {
                fileOutputStream.write(buffer, 0, len);
                bytesWritten += len;
                fileOutputStream.close();

                // Check if writer is still relevant
                if (storageWriterFuture.isDone()) {
                  throw new SnapshotTransferException(
                      "Error writing snapshot to backend, check previous log messages for more details. Aborting download and unzip process");
                }

                // Add the part to the queue
                part.setSize(bytesWritten);
                partQueue.put(part);

                // Prep for the next part
                bytesWritten = 0;
                filePath = Files.createTempFile(Paths.get("/var/tmp"), snapshotId + '-', '-' + String.valueOf(++partNumber));
                fileOutputStream = new FileOutputStream(filePath.toString());
                part = new SnapshotPart();
                part.setFileName(filePath.toString());
                part.setPartNumber(partNumber);
                part.setIsLast(Boolean.FALSE);
              }
            }

            // Close the streams and free the resources
            gzipInputStream.close();
            fileOutputStream.close();
            buffer = null;

            // Add the last part to the queue
            part.setSize(bytesWritten);
            part.setIsLast(Boolean.TRUE);
            partQueue.put(part);

            if (StringUtils.isNotBlank(storageWriterFuture.get(transferTimeout, TimeUnit.HOURS))) {
              LOG.info("Downloaded snapshot " + snapshotId + " to storage backend");
            } else {
              throw new SnapshotTransferException("Failed to download snapshot " + snapshotId + " to storage backend");
            }
          } catch (Exception e) {
            // Clean up the files that were created
            try {
              if (!storageWriterFuture.isDone()) {
                storageWriterFuture.cancel(true);
              }
              List<SnapshotPart> remainingParts = new ArrayList<SnapshotPart>();
              partQueue.drainTo(remainingParts);
              for (SnapshotPart part : remainingParts) {
                deleteFile(part.getFileName());
              }
            } catch (Exception ex) {
              LOG.warn("Unable to clean up artifacts left by a failed attempt to download " + snapshotId + " to storage backend", ex);
            }
            throw e;
          } finally {
            try {
              if (fileOutputStream != null)
                fileOutputStream.close();
            } catch (Exception e) {

            }
          }
        }
      } catch (SnapshotTransferException e) {
        throw e;
      } catch (Exception e) {
        throw new SnapshotTransferException("Failed to download snapshot " + snapshotId + " to storage backend", e);
      } finally {
        try {
          if (gzipInputStream != null)
            gzipInputStream.close();
        } catch (Exception e) {

        }
        try {
          snapObj.getObjectContent().close();
        } catch (Exception e) {

        }
      }
    } else {
      LOG.warn("No snapshot content available from objectstorage gateway: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
      throw new SnapshotTransferException("No snapshot content available: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
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
      throw new SnapshotTransferException("Failed to delete snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName
          + ", key=" + keyName, e);
    }
  }

  @Override
  public Long getSizeInBytes() throws SnapshotTransferException {
    LOG.debug("Fetching snapshot metadata from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);

    validateInput();
    ObjectMetadata metadata = null;
    Map<String, String> userMetadata = null;
    try {
      metadata = retryAfterRefresh(new Function<GetObjectMetadataRequest, ObjectMetadata>() {

        @Override
        @Nullable
        public ObjectMetadata apply(@Nullable GetObjectMetadataRequest arg0) {
          eucaS3Client.refreshEndpoint();
          return eucaS3Client.getObjectMetadata(arg0);
        }

      }, new GetObjectMetadataRequest(bucketName, keyName), REFRESH_TOKEN_RETRIES);
    } catch (Exception e) {
      LOG.warn("Failed to get snapshot metadata from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
      throw new SnapshotTransferException("Failed to get snapshot metadata from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName
          + ", key=" + keyName, e);
    }

    if (metadata != null && (userMetadata = metadata.getUserMetadata()) != null && userMetadata.containsKey(UNCOMPRESSED_SIZE_KEY)) {
      try {
        return Long.parseLong(userMetadata.get(UNCOMPRESSED_SIZE_KEY));
      } catch (Exception e) {
        throw new SnapshotTransferException("Unable to parse size from snapshot metadata: snapshotId=" + snapshotId + ", bucket=" + bucketName
            + ", key=" + keyName + ", metadata key:value pair=" + UNCOMPRESSED_SIZE_KEY + ":" + userMetadata.get(UNCOMPRESSED_SIZE_KEY), e);
      }
    } else {
      throw new SnapshotTransferException("Snapshot metadata from objectstorage does not contain uncompressed size: snapshotId=" + snapshotId
          + ", bucket=" + bucketName + ", key=" + keyName);
    }
  }

  private void initializeEucaS3Client() throws SnapshotTransferException {
    if (role == null) {
      try {
        role = BlockStorageUtil.getBlockStorageRole();
      } catch (Exception e) {
        LOG.error("Failed to initialize account for snapshot transfers due to " + e);
        throw new SnapshotTransferException("Failed to initialize eucalyptus account for snapshot transfers", e);
      }
    }

    try {
      eucaS3Client = EucaS3ClientFactory.getEucaS3ClientByRole(role, "snapshot-transfer", (int) TimeUnit.HOURS.toSeconds(1));
    } catch (Exception e) {
      LOG.error("Failed to initialize S3 client for snapshot transfers due to " + e);
      throw new SnapshotTransferException("Failed to initialize S3 client for snapshot transfers", e);
    }
  }

  private void loadTransferConfig() {
    StorageInfo info = StorageInfo.getStorageInfo();
    this.partSize = (long) (info.getSnapshotPartSizeInMB() * 1024 * 1024);
    this.queueSize = info.getMaxSnapshotPartsQueueSize();
    this.transferTimeout = info.getSnapshotTransferTimeoutInHours();
    this.readBufferSize = info.getReadBufferSizeInMB() * 1024 * 1024;
    this.writeBufferSize = info.getWriteBufferSizeInMB() * 1024 * 1024;
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
            bucket =
                SnapshotTransferConfiguration.updateBucketName(
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
            bucket =
                SnapshotTransferConfiguration.updateBucketName(
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

  private S3Object download() throws SnapshotTransferException {
    try {
      LOG.debug("Downloading snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
      return retryAfterRefresh(new Function<GetObjectRequest, S3Object>() {

        @Override
        @Nullable
        public S3Object apply(@Nullable GetObjectRequest arg0) {
          eucaS3Client.refreshEndpoint();
          return eucaS3Client.getObject(arg0);
        }

      }, new GetObjectRequest(bucketName, keyName), REFRESH_TOKEN_RETRIES);
    } catch (Exception e) {
      LOG.warn("Failed to download snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName);
      throw new SnapshotTransferException("Failed to download snapshot from objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName
          + ", key=" + keyName, e);
    }
  }

  private String uploadSnapshotAsSingleObject(final String compressedSnapFileName, Long actualSize, Long uncompressedSize,
      final SnapshotProgressCallback callback) throws SnapshotUploadObjectException {
    callback.setUploadSize(actualSize);
    ObjectMetadata objectMetadata = new ObjectMetadata();
    Map<String, String> userMetadataMap = new HashMap<String, String>();
    userMetadataMap.put(UNCOMPRESSED_SIZE_KEY, String.valueOf(uncompressedSize)); // Send the uncompressed length as the metadata
    objectMetadata.setUserMetadata(userMetadataMap);
    objectMetadata.setContentLength(actualSize);

    try {
      LOG.debug("Uploading " + compressedSnapFileName);
      PutObjectResult putResult = retryAfterRefresh(new Function<PutObjectRequest, PutObjectResult>() {

        @Override
        @Nullable
        public PutObjectResult apply(@Nullable PutObjectRequest arg0) {
          eucaS3Client.refreshEndpoint();
          // EUCA-10311 Set the input stream in put request. Doing it here to ensure that input stream is set before every attempt to put object
          try {
            arg0.setInputStream(new FileInputStreamWithCallback(new File(compressedSnapFileName), callback));
          } catch (Exception e) {
            LOG.warn("Failed to upload snapshot to objectstorage: snapshotId=" + snapshotId + ", bucket=" + bucketName + ", key=" + keyName
                + ", reason: unable to initialize FileInputStreamWithCallback for file " + compressedSnapFileName, e);
            Exceptions.toUndeclared(e);
          }
          return eucaS3Client.putObject(arg0);
        }

      }, new PutObjectRequest(bucketName, keyName, null, objectMetadata), REFRESH_TOKEN_RETRIES);

      return putResult.getETag();
    } catch (Exception e) {
      LOG.warn("Failed to upload object " + compressedSnapFileName, e);
      throw new SnapshotUploadObjectException("Failed to upload object " + compressedSnapFileName, e);
    } finally {
      deleteFile(compressedSnapFileName);
    }
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
      LOG.debug("Inititating multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName);
      initResponse = retryAfterRefresh(new Function<InitiateMultipartUploadRequest, InitiateMultipartUploadResult>() {

        @Override
        @Nullable
        public InitiateMultipartUploadResult apply(@Nullable InitiateMultipartUploadRequest arg0) {
          eucaS3Client.refreshEndpoint();
          return eucaS3Client.initiateMultipartUpload(arg0);
        }

      }, initRequest, REFRESH_TOKEN_RETRIES);
    } catch (Exception ex) {
      throw new SnapshotInitializeMpuException("Failed to initialize multipart upload part for snapshotId=" + snapshotId + ", bucketName="
          + bucketName + ", keyName=" + keyName, ex);
    }

    if (StringUtils.isBlank(initResponse.getUploadId())) {
      throw new SnapshotInitializeMpuException("Invalid upload ID for multipart upload part for snapshotId=" + snapshotId + ", bucketName="
          + bucketName + ", keyName=" + keyName);
    }
    return initResponse.getUploadId();
  }

  private PartETag uploadPart(SnapshotPart part) throws SnapshotUploadPartException {
    try {
      part = part.updateStateUploading();
    } catch (Exception e) {
      LOG.debug("Failed to update part status in DB. Moving on. " + part);
    }

    try {
      LOG.debug("Uploading " + part);
      UploadPartResult uploadPartResult =
          retryAfterRefresh(new Function<UploadPartRequest, UploadPartResult>() {

            @Override
            @Nullable
            public UploadPartResult apply(@Nullable UploadPartRequest arg0) {
              eucaS3Client.refreshEndpoint();
              return eucaS3Client.uploadPart(arg0);
            }
          },
              new UploadPartRequest().withBucketName(part.getBucketName()).withKey(part.getKeyName()).withUploadId(part.getUploadId())
                  .withPartNumber(part.getPartNumber()).withPartSize(part.getSize()).withFile(new File(part.getFileName())), REFRESH_TOKEN_RETRIES);

      return uploadPartResult.getPartETag();
    } catch (Exception e) {
      LOG.warn("Failed to upload part " + part, e);
      throw new SnapshotUploadPartException("Failed to upload part " + part, e);
    } finally {
      deleteFile(part.getFileName());
    }
  }

  private String finalizeMultipartUpload(List<PartETag> partETags) throws SnapshotFinalizeMpuException {
    CompleteMultipartUploadResult result;
    try {
      LOG.debug("Finalizing multipart upload: snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName + ", uploadId="
          + uploadId);
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

  public static abstract class UploadPart implements Callable<List<PartETag>> {
  }

  class UploadPartTask extends UploadPart {

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
      Boolean error = Boolean.FALSE;
      Boolean isLast = Boolean.FALSE;

      try {
        do {
          SnapshotPart part = null;

          try {
            part = partQueue.take();
          } catch (InterruptedException ex) { // Should rarely happen
            error = Boolean.TRUE;
            LOG.error("Failed to upload snapshot " + snapshotId + " due to an retrieving parts from queue", ex);
            return null;
          }

          if (part != null) {
            if (part.getState().equals(SnapshotPartState.created) || part.getState().equals(SnapshotPartState.uploading)
                || part.getState().equals(SnapshotPartState.failed)) {
              isLast = part.getIsLast();
              try {
                PartETag partEtag = uploadPart(part);
                partETags.add(partEtag);

                progressCallback.updateUploadProgress(part.getInputFileBytesRead());
                try {
                  part = part.updateStateUploaded(partEtag.getETag());
                } catch (Exception e) {
                  LOG.debug("Failed to update part status in DB. Moving on. " + part);
                }

                LOG.debug("Uploaded " + part);
              } catch (Exception e) {
                error = Boolean.TRUE;
                // update part status in database
                try {
                  part = part.updateStateFailed();
                } catch (Throwable t) {
                  LOG.debug("Failed to update part status in DB for " + part, t);
                }
                return null;
              }
            } else {
              LOG.warn("Not sure what to do with part in state " + part.getState() + ". Ignoring " + part);
            }
          } else {
            error = Boolean.TRUE;
            LOG.warn("Null reference snapshot part found in queue for " + snapshotId + ". Aborting snapshot upload");
            return null;
          }
        } while (!isLast);

        return partETags;
      } catch (Throwable t) {
        error = Boolean.TRUE;
        LOG.warn("Failed to process snapshot uplodad for " + snapshotId, t);
        return null;
      } finally {
        if (error && partQueue != null) { // drain the queue so the upload process does not hang
          LOG.debug("Clearing part queue for " + snapshotId + " due to a previous error uploading");
          partQueue.clear();
        }
      }
    }
  }

  public static abstract class CompleteUpload implements Callable<String> {
  }

  class CompleteMpuTask extends CompleteUpload {

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
        List<PartETag> partETags = uploadTaskFuture.get(transferTimeout, TimeUnit.HOURS);
        if (partETags != null && partETags.size() == totalParts) {
          try {
            etag = finalizeMultipartUpload(partETags);
            // markSnapshotAvailable();
            try {
              snapUploadInfo = snapUploadInfo.updateStateUploaded(etag);
            } catch (Exception e) {
              LOG.debug("Failed to update status in DB for " + snapUploadInfo);
            }
            LOG.debug("Uploaded snapshot " + snapUploadInfo.getSnapshotId() + " to objectstorage");
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
        LOG.error("Failed to upload " + snapshotId + ". Complete upload task timed out waiting on upload part task after " + transferTimeout
            + " hours");
      } catch (Exception ex) {
        error = Boolean.TRUE;
        LOG.error("Failed to upload " + snapshotId, ex);
      } finally {
        if (error) {
          abortUpload(snapUploadInfo);
          etag = null;
        }
      }
      return etag;
    }
  }

  class UploadObjectTask extends CompleteUpload {

    private SnapshotPart part;
    private SnapshotUploadInfo snapUploadInfo;
    private String pathToFile;
    private Long actualSize;
    private Long uncompressedSize;
    private SnapshotProgressCallback callback;

    public UploadObjectTask(SnapshotPart part, SnapshotUploadInfo snapUploadInfo, String pathToFile, Long actualSize, Long uncompressedSize,
        SnapshotProgressCallback callback) {
      this.part = part;
      this.snapUploadInfo = snapUploadInfo;
      this.pathToFile = pathToFile;
      this.actualSize = actualSize;
      this.uncompressedSize = uncompressedSize;
      this.callback = callback;
    }

    @Override
    public String call() throws Exception {
      String etag = null;
      try {
        etag = uploadSnapshotAsSingleObject(pathToFile, actualSize, uncompressedSize, callback);
        try {
          part = part.updateStateUploaded(etag);
          snapUploadInfo = snapUploadInfo.updateStateUploaded(etag);
        } catch (Exception e) {
          LOG.debug("Failed to update status in DB for " + snapUploadInfo);
        }
        LOG.debug("Uploaded " + pathToFile + " to objectstorage");
      } catch (Exception e) {
        LOG.warn("Failed to upload " + snapshotId, e);
        abortUpload(snapUploadInfo);
      }
      return etag;
    }
  }

  public static abstract class StorageWriter implements Callable<String> {
  }

  class StorageWriterTask extends StorageWriter {

    private ArrayBlockingQueue<SnapshotPart> partQueue;
    private StorageResource storageResource;

    public StorageWriterTask(ArrayBlockingQueue<SnapshotPart> partQueue, StorageResource storageResource) {
      this.partQueue = partQueue;
      this.storageResource = storageResource;
    }

    @Override
    public String call() throws Exception {
      String returnValue = null;
      SnapshotPart part = null;
      OutputStream outStream = null;
      byte[] buffer = new byte[writeBufferSize];
      int len;

      try {
        outStream = storageResource.getOutputStream();
        do {
          part = partQueue.take();

          FileInputStream inStream = null;
          try {
            inStream = new FileInputStream(part.getFileName());
            while ((len = inStream.read(buffer)) > 0) {
              outStream.write(buffer, 0, len);
            }
            inStream.close();
          } finally {
            if (inStream != null) {
              try {
                inStream.close();
              } catch (Exception e) {

              }
            }
            deleteFile(part.getFileName());
          }
        } while (!part.getIsLast());

        outStream.close();
        buffer = null;

        returnValue = storageResource.getId();
      } catch (Exception e) {
        LOG.error("Failed to write snapshot " + snapshotId + " to storage backend due to:", e);
      } finally {
        if (outStream != null) {
          try {
            outStream.close();
          } catch (Exception e) {

          }
        }
      }

      return returnValue;
    }
  }
}
