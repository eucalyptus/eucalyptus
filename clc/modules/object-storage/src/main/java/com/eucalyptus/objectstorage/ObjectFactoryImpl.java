/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage;

import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.primitives.Ints.tryParse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.s3.AccountProblemException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.metadata.ObjectMetadataManager;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.config.ConfigurationCache;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ObjectFactoryImpl implements ObjectFactory {
  private static final Logger LOG = Logger.getLogger(ObjectFactoryImpl.class);

  /*
   * The thread pool to handle the PUT operations to the backend. Use another thread to allow status updates on the object entity in the db to renew
   * the lease to prevent OSG object GC from occuring while the object is still uploading.
   */
  private static final boolean DEFAULT_CORE_THREADS_TIMEOUT = true;
  private static final int DEFAULT_CORE_POOL_SIZE = 100;
  private static final int DEFAULT_MAX_POOL_SIZE = 100;
  private static final int DEFAULT_MAX_QUEUE_SIZE = 2 * DEFAULT_MAX_POOL_SIZE;

  private static final String PROP_PREFIX = "com.eucalyptus.objectstorage.putService";
  private static final boolean CORE_THREADS_TIMEOUT =
      Boolean.valueOf( getProperty( PROP_PREFIX + "CoreThreadsTimeout", valueOf( DEFAULT_CORE_THREADS_TIMEOUT ) ) );
  private static final int CORE_POOL_SIZE =
      firstNonNull( tryParse( getProperty( PROP_PREFIX + "CoreThreads", valueOf( DEFAULT_CORE_POOL_SIZE ) ) ), DEFAULT_CORE_POOL_SIZE);
  private static final int MAX_POOL_SIZE =
      firstNonNull( tryParse( getProperty( PROP_PREFIX + "MaxThreads", valueOf( DEFAULT_MAX_POOL_SIZE ) ) ), DEFAULT_MAX_POOL_SIZE);
  private static final int MAX_QUEUE_SIZE =
      firstNonNull( tryParse( getProperty( PROP_PREFIX + "QueueSize", valueOf( DEFAULT_MAX_QUEUE_SIZE ) ) ), DEFAULT_MAX_QUEUE_SIZE);
  private static final ExecutorService PUT_OBJECT_SERVICE;

  static {
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
        Threads.threadFactory( "osg-object-factory-pool-%d" )
    );
    executor.allowCoreThreadTimeOut( CORE_THREADS_TIMEOUT );
    PUT_OBJECT_SERVICE = executor;
  }

  public static long getPutTimeoutInMillis() {
    return ConfigurationCache.getConfiguration(ObjectStorageGlobalConfiguration.class).getFailed_put_timeout_hrs() * 60l * 60l * 1000l;
  }

  public static boolean useGetPutOnCopy() {
    try {
      return ConfigurationCache.getConfiguration(ObjectStorageGlobalConfiguration.class).getDoGetPutOnCopyFail();
    } catch (Throwable f) {
      LOG.error("Error getting OSG configuration for get/put on copy fail. Falling back to fail the operation", f);
      return false;
    }
  }

  @Override
  public ObjectEntity copyObject(@Nonnull final ObjectStorageProviderClient provider, @Nonnull ObjectEntity entity,
      @Nonnull final CopyObjectType request, @Nonnull final User requestUser, final String metadataDirective) throws S3Exception {
    final ObjectMetadataManager objectManager = ObjectMetadataManagers.getInstance();

    // Initialize metadata for the object
    if (BucketState.extant.equals(entity.getBucket().getState())) {
      // Initialize the object metadata.
      try {
        if (!ObjectState.extant.equals(entity.getState())) {
          entity = objectManager.initiateCreation(entity);
        }
      } catch (Exception e) {
        LOG.warn("Error initiating an object in the db:", e);
        throw new InternalErrorException(entity.getResourceFullName());
      }
    } else {
      throw new NoSuchBucketException(entity.getBucket().getBucketName());
    }

    final String etag;
    Date lastMod;
    CopyObjectResponseType response;

    try {
      final ObjectEntity uploadingObject = entity;

      Callable<CopyObjectResponseType> putCallable = new Callable<CopyObjectResponseType>() {

        @Override
        public CopyObjectResponseType call() throws Exception {
          LOG.debug("calling copyObject");
          CopyObjectResponseType response;
          try {
            response = provider.copyObject(request);
          } catch (Exception ex) {
            if (useGetPutOnCopy()) {
              response = providerGetPut(provider, request, requestUser, metadataDirective);
            } else {
              LOG.warn("Exception caught while attempting to copy object on backend");
              throw ex;
            }
          }
          LOG.debug("Done with copyObject. " + response.getStatusMessage());
          return response;
        }
      };

      // Send the data
      final FutureTask<CopyObjectResponseType> putTask = new FutureTask<>(putCallable);
      PUT_OBJECT_SERVICE.execute(putTask);
      final long failTime = System.currentTimeMillis() + getPutTimeoutInMillis();
      final long checkIntervalSec = ObjectStorageProperties.OBJECT_CREATION_EXPIRATION_INTERVAL_SEC / 2;
      final AtomicReference<ObjectEntity> entityRef = new AtomicReference<>(uploadingObject);
      Callable updateTimeout = new Callable() {
        @Override
        public Object call() throws Exception {
          ObjectEntity tmp = entityRef.get();
          try {
            entityRef.getAndSet(ObjectMetadataManagers.getInstance().updateCreationTimeout(tmp));
          } catch (Exception ex) {
            LOG.warn("Could not update the creation expiration time for ObjectUUID " + tmp.getObjectUuid() + " Will retry next interval", ex);
          }
          return entityRef.get();
        }
      };
      response = waitForCompletion(putTask, uploadingObject.getObjectUuid(), updateTimeout, failTime, checkIntervalSec);
      etag = response.getEtag();
      // right now the time between walrus's response and object creation is not long enough that ObjectMetadataManager.cleanupInvalidObjects
      // can tell which object is the latest if (for instance) a delete and copyObject are called subsequently
      // TODO - try to honor upstream last modified date in the future by refactoring OMM.cleanupInvalidObjects not to depend on timestamps
      // lastMod = DateFormatter.dateFromListingFormattedString(response.getLastModified());
      lastMod = new Date();
    } catch (Exception e) {
      LOG.error("Data PUT failure to backend for bucketuuid / objectuuid : " + entity.getBucket().getBucketUuid() + "/" + entity.getObjectUuid(), e);

      // Remove metadata and return failure
      try {
        ObjectMetadataManagers.getInstance().transitionObjectToState(entity, ObjectState.deleting);
      } catch (Exception ex) {
        LOG.warn("Failed to mark failed object entity in deleting state on failure rollback. Will be cleaned later.", e);
      }

      // ObjectMetadataManagers.getInstance().delete(objectEntity);
      throw new InternalErrorException(entity.getObjectKey());
    }

    try {
      // fireRepairTask(bucket, savedEntity.getObjectKey());
      // Update metadata to "extant". Retry as necessary
      return ObjectMetadataManagers.getInstance().finalizeCreation(entity, lastMod, etag);
    } catch (Exception e) {
      LOG.warn("Failed to update object metadata for finalization. Failing PUT operation", e);
      throw new InternalErrorException(entity.getResourceFullName());
    }
  }

  private CopyObjectResponseType providerGetPut(final ObjectStorageProviderClient provider, final CopyObjectType request, final User requestUser,
      final String metadataDirective) throws InternalErrorException {
    GetObjectType got = new GetObjectType(request.getSourceBucket(), request.getSourceObject(), Boolean.FALSE, Boolean.FALSE);
    GetObjectResponseType gort = null;
    try {
      gort = provider.getObject(got);
    } catch (S3Exception e) {
      LOG.error("while attempting to copy an object on a backend that does not support copy, an exception "
          + "was thrown trying to GET the source object", e);
      return null;
    }

    InputStream sourceObjData = gort.getDataInputStream();
    PutObjectType pot = new PutObjectType();
    pot.setBucket(request.getDestinationBucket());
    pot.setKey(request.getDestinationObject());
    pot.setMetaData(gort.getMetaData());
    pot.setUser(requestUser);
    pot.setContentLength(gort.getSize().toString());
    if (metadataDirective != null && "REPLACE".equals(metadataDirective)) {
      pot.setMetaData(request.getMetaData());
    } else if (metadataDirective == null || "".equals(metadataDirective) || "COPY".equals(metadataDirective)) {
      pot.setMetaData(gort.getMetaData());
    } else {
      throw new InternalErrorException(null, "Could not copy " + request.getSourceBucket() + "/" + request.getSourceObject() + " to "
          + request.getDestinationBucket() + "/" + request.getDestinationObject() + " on the backend because the metadata directive not recognized");
    }
    PutObjectResponseType port = null;
    try {
      port = provider.putObject(pot, sourceObjData);
    } catch (S3Exception e) {
      LOG.error("while attempting to copy an object on a backend that does not support copy, an exception "
          + "was thrown trying to PUT the destination object in the backend", e);
      return null;
    }
    CopyObjectResponseType response = new CopyObjectResponseType();
    response.setVersionId(port.getVersionId());
    response.setKey(request.getDestinationObject());
    response.setBucket(request.getDestinationBucket());
    response.setStatusMessage(port.getStatusMessage());
    response.setEtag(port.getEtag());
    response.setMetaData(port.getMetaData());
    // Last modified date in copy response is in ISO8601 format as per S3 API
    response.setLastModified(DateFormatter.dateToListingFormattedString(port.getLastModified()));
    return response;
  }

  @Override
  public ObjectEntity createObject(@Nonnull final ObjectStorageProviderClient provider, @Nonnull ObjectEntity entity,
      @Nonnull final InputStream content, @Nullable final List<MetaDataEntry> userMetadata, @Nonnull final User requestUser) throws S3Exception {

    final ObjectMetadataManager objectManager = ObjectMetadataManagers.getInstance();

    // Initialize metadata for the object
    if (BucketState.extant.equals(entity.getBucket().getState())) {
      // Initialize the object metadata.
      try {
        entity = objectManager.initiateCreation(entity);
      } catch (Exception e) {
        LOG.warn("Error initiating an object in the db:", e);
        throw new InternalErrorException(entity.getResourceFullName());
      }
    } else {
      throw new NoSuchBucketException(entity.getBucket().getBucketName());
    }

    final Date lastModified;
    final String etag;
    PutObjectResponseType response;

    try {
      final ObjectEntity uploadingObject = entity;
      final PutObjectType putRequest = new PutObjectType();
      putRequest.setBucket(uploadingObject.getBucket().getBucketUuid());
      putRequest.setKey(uploadingObject.getObjectUuid());
      putRequest.setUser(requestUser);
      putRequest.setContentLength(entity.getSize().toString());
      putRequest.setMetaData(userMetadata==null ? null : Lists.newArrayList(userMetadata));

      Callable<PutObjectResponseType> putCallable = new Callable<PutObjectResponseType>() {

        @Override
        public PutObjectResponseType call() throws Exception {
          LOG.debug("Putting data");
          PutObjectResponseType response = provider.putObject(putRequest, content);
          LOG.debug("Done with put. Response status: " + response.getStatusMessage());
          return response;
        }
      };

      // Send the data
      final FutureTask<PutObjectResponseType> putTask = new FutureTask<>(putCallable);
      PUT_OBJECT_SERVICE.execute(putTask);
      final long failTime = System.currentTimeMillis() + getPutTimeoutInMillis();
      final long checkIntervalSec = ObjectStorageProperties.OBJECT_CREATION_EXPIRATION_INTERVAL_SEC / 2;
      final AtomicReference<ObjectEntity> entityRef = new AtomicReference<>(uploadingObject);
      Callable updateTimeout = new Callable() {
        @Override
        public Object call() throws Exception {
          ObjectEntity tmp = entityRef.get();
          try {
            entityRef.getAndSet(ObjectMetadataManagers.getInstance().updateCreationTimeout(tmp));
          } catch (Exception ex) {
            LOG.warn("Could not update the creation expiration time for ObjectUUID " + tmp.getObjectUuid() + " Will retry next interval", ex);
          }
          return entityRef.get();
        }
      };
      response = waitForCompletion(putTask, uploadingObject.getObjectUuid(), updateTimeout, failTime, checkIntervalSec);
      entity = entityRef.get(); // Get the latest if it was updated
      // lastModified = response.getLastModified();
      lastModified = new Date();
      etag = response.getEtag();

    } catch (Exception e) {
      LOG.error("Data PUT failure to backend for bucketuuid / objectuuid : " + entity.getBucket().getBucketUuid() + "/" + entity.getObjectUuid(), e);

      // Remove metadata and return failure
      try {
        ObjectMetadataManagers.getInstance().transitionObjectToState(entity, ObjectState.deleting);
      } catch (Exception ex) {
        LOG.warn("Failed to mark failed object entity in deleting state on failure rollback. Will be cleaned later.", e);
      }

      // ObjectMetadataManagers.getInstance().delete(objectEntity);
      throw new InternalErrorException(entity.getObjectKey());
    }

    try {
      // fireRepairTask(bucket, savedEntity.getObjectKey());
      // Update metadata to "extant". Retry as necessary
      return ObjectMetadataManagers.getInstance().finalizeCreation(entity, lastModified, etag);
    } catch (Exception e) {
      LOG.warn("Failed to update object metadata for finalization. Failing PUT operation", e);
      throw new InternalErrorException(entity.getResourceFullName());
    }
  }

  /**
   * Wait for the upload task to complete to the backend. Update the creationExpiration time on intervals to ensure that other OSGs don't mistake the
   * upload as failed.
   *
   * @return
   * @throws Exception
   */
  private <T extends BaseMessage> T waitForCompletion(@Nonnull Future<T> pendingTask, String objectUuid, @Nonnull Callable timeoutUpdate,
      final long failOperationTimeSec, final long checkIntervalSec) throws Exception {
    T response;
    // Final time to wait before declaring failure.

    while (System.currentTimeMillis() < failOperationTimeSec * 1000) {
      try {
        response = pendingTask.get(checkIntervalSec, TimeUnit.SECONDS);
        return response;
      } catch (TimeoutException e) {
        // fall thru and retry
        timeoutUpdate.call();

      } catch (CancellationException e) {
        LOG.debug("PUT operation cancelled for object/part UUID " + objectUuid);
        throw e;
      } catch (ExecutionException e) {
        LOG.debug("PUT operation failed due to exception. object/part UUID " + objectUuid, e);
        throw e;
      } catch (InterruptedException e) {
        LOG.warn("PUT operation interrupted. Object/Part UUID " + objectUuid, e);
        throw e;
      }
    }

    // Big fail. This should not happen. Means the upload lasted 24hrs or more
    throw new Exception("Timed out on upload");
  }

  private <T extends ObjectStorageDataResponseType> T waitForMultipartCompletion(@Nonnull Future<T> pendingTask, @Nonnull String uploadId,
      @Nonnull String correlationId, final long failOperationTimeSec, final long checkIntervalSec) throws Exception {
    T response;
    // Final time to wait before declaring failure.

    while (System.currentTimeMillis() < failOperationTimeSec * 1000) {
      try {
        response = pendingTask.get(checkIntervalSec, TimeUnit.SECONDS);
        return response;
      } catch (TimeoutException e) {
        OSGChannelWriter.writeResponse(Contexts.lookup(correlationId), OSGMessageResponse.Whitespace);
      } catch (CancellationException | ExecutionException | InterruptedException e) {
        LOG.debug("Complete upload operation failed for upload ID: " + uploadId, e);
        throw e;
      }
    }

    // Big fail. This should not happen. Means the upload lasted 24hrs or more
    throw new Exception("Timed out on upload");
  }

  @Override
  public ObjectEntity logicallyDeleteVersion(@Nonnull ObjectStorageProviderClient provider, @Nonnull ObjectEntity entity, @Nonnull User requestUser)
      throws S3Exception {
    ObjectEntity toBeReturned = null;

    if (entity.getBucket() == null) {
      throw new InternalErrorException();
    }
    try {
      List<ObjectEntity> entities =
          ObjectMetadataManagers.getInstance().lookupObjectVersions(entity.getBucket(), entity.getObjectKey(), Integer.MAX_VALUE);
      if (entities != null && entities.size() > 0) {
        for (ObjectEntity latest : entities) {
          if (latest.getObjectUuid().equals(entity.getObjectUuid())) {
            continue;
          }
          if (!latest.getIsLatest().booleanValue()) {
            ObjectMetadataManagers.getInstance().makeLatest(latest);
          }
          break;
        }
      }
    } catch (Exception ex) {
      LOG.warn("while attempting to set isLatest = true on the newest remaining object version, an exception was encountered: ", ex);
    }
    if (!entity.getIsDeleteMarker()) {
      toBeReturned = ObjectMetadataManagers.getInstance().transitionObjectToState(entity, ObjectState.deleting);

      // Optimistically try to actually delete the object, failure here is okay
      try {
        actuallyDeleteObject(provider, toBeReturned, requestUser);
      } catch (Exception e) {
        LOG.trace("Could not delete the object in the sync path, will retry later asynchronosly. Object now in state 'deleting'.", e);
      }
    } else {
      // Delete the delete marker and return it
      toBeReturned = entity;
      ObjectMetadataManagers.getInstance().delete(entity);
    }

    return toBeReturned;
  }

  @Override
  public ObjectEntity logicallyDeleteObject(@Nonnull ObjectStorageProviderClient provider, @Nonnull ObjectEntity entity, @Nonnull UserPrincipal requestUser)
      throws S3Exception {
    ObjectEntity toBeReturned = null;

    if (entity.getBucket() == null) {
      throw new InternalErrorException();
    }

    switch (entity.getBucket().getVersioning()) {
      case Suspended:
      case Enabled:
        // EUCA-9983 - If bucket versioning is enabled/suspended, a new delete marker should be returned every time the object is deleted
        try {
          // Create a "private" acp for the delete marker
          AccessControlPolicy acp = AclUtils.processNewResourcePolicy(requestUser, null, entity.getBucket().getOwnerCanonicalId());
          // Create new deleteMarker
          toBeReturned = ObjectMetadataManagers.getInstance().generateAndPersistDeleteMarker(entity, acp, requestUser);
        } catch (Exception e) {
          LOG.warn("Failure configuring and persisting the delete marker for object " + entity.getResourceFullName());
          throw new InternalErrorException(e);
        }
        break;
      case Disabled:
        // Cannot be a delete marker, so this is proper. Return a null object entity, version/delete don't exist in this case
        logicallyDeleteVersion(provider, entity, requestUser);
        break;
      default:
        LOG.error("Cannot logically delete object due to unexpected bucket state found: " + entity.getBucket().getVersioning());
        throw new InternalErrorException(entity.getBucket().getName());
    }

    return toBeReturned;
  }

  @Override
  public void actuallyDeleteObject(@Nonnull ObjectStorageProviderClient provider, @Nonnull ObjectEntity entity, @Nullable User requestUser)
      throws S3Exception {

    if (!ObjectState.deleting.equals(entity.getState())) {
      try {
        entity = ObjectMetadataManagers.getInstance().transitionObjectToState(entity, ObjectState.deleting);
      } catch (Exception e) {
        LOG.debug("Could not mark metadata for deletion", e);
        throw e;
      }
    }

    if (entity.getIsDeleteMarker()) {
      // Delete markers are just removed, no backend call needed.
      ObjectMetadataManagers.getInstance().delete(entity);
      return;
    }

    // Issue delete to backend
    DeleteObjectType deleteRequest;
    DeleteObjectResponseType deleteResponse;
    LOG.trace("Deleting object " + entity.getObjectUuid() + ".");
    deleteRequest = new DeleteObjectType();

    // Always use the system admin for deletions if not given an explicit user
    if (requestUser == null) {
      try {
        requestUser = Accounts.lookupSystemAdmin();
      } catch (AuthException e) {
        LOG.trace("System admin account not found for object deletion. Cannot remove object with uuid " + entity.getObjectUuid());
        throw new AccountProblemException("Eucalyptus/Admin");
      }
    }

    try {
      deleteRequest.setUser(requestUser);
      deleteRequest.setBucket(entity.getBucket().getBucketUuid());
      deleteRequest.setKey(entity.getObjectUuid());

      try {
        deleteResponse = provider.deleteObject(deleteRequest);
        if (!(HttpResponseStatus.NO_CONTENT.equals(deleteResponse.getStatus()) || HttpResponseStatus.OK.equals(deleteResponse.getStatus()))) {
          LOG.trace("Backend did not confirm deletion of " + deleteRequest.getBucket() + "/" + deleteRequest.getKey() + " via request: "
              + deleteRequest.toString());
          throw new Exception("Object could not be confirmed as deleted.");
        }
      } catch (S3Exception e) {
        if (HttpResponseStatus.NOT_FOUND.equals(e.getStatus())) {
          // Ok, fall through.
        } else {
          throw e;
        }
      }

      // Object does not exist on backend, remove record
      Transactions.delete(entity);

    } catch (EucalyptusCloudException ex) {
      // Failed. Keep record so we can retry later
      LOG.trace("Error in response from backend on deletion request for object on backend: " + deleteRequest.getBucket() + "/"
          + deleteRequest.getKey());
    } catch (Exception e) {
      LOG.warn("Error deleting object on backend. Will retry later", e);
    }
  }

  /**
   * Create a multipart Upload (get an Id from the backend and initialize the metadata. Returns a persisted uploadId record as an ObjectEntity with
   * the uploadId in state 'mpu-pending'
   *
   * @param provider
   * @param upload
   * @param requestUser
   * @return
   * @throws S3Exception
   */
  @Override
  public ObjectEntity createMultipartUpload(final ObjectStorageProviderClient provider, ObjectEntity upload, User requestUser) throws S3Exception {
    final ObjectMetadataManager objectManager = ObjectMetadataManagers.getInstance();

    // Initialize metadata for the object
    if (BucketState.extant.equals(upload.getBucket().getState())) {
      // Initialize the object metadata.
      try {
        upload = objectManager.initiateCreation(upload);
      } catch (Exception e) {
        LOG.warn("Error initiating an object in the db:", e);
        throw new InternalErrorException(upload.getResourceFullName());
      }
    } else {
      throw new NoSuchBucketException(upload.getBucket().getBucketName());
    }

    try {
      final InitiateMultipartUploadType initRequest = new InitiateMultipartUploadType();
      initRequest.setBucket(upload.getBucket().getBucketUuid());
      initRequest.setKey(upload.getObjectUuid());
      initRequest.setUser(requestUser);
      initRequest.setStorageClass(upload.getStorageClass());
      initRequest.setAccessControlList(upload.getAccessControlPolicy().getAccessControlList());

      LOG.trace("Initiating MPU on backend");
      InitiateMultipartUploadResponseType response = provider.initiateMultipartUpload(initRequest);
      upload.setObjectModifiedTimestamp(response.getLastModified());
      upload.setUploadId(response.getUploadId());
      LOG.trace("Done with MPU init on backend. " + response.getStatusMessage());
    } catch (Exception e) {
      LOG.error("InitiateMPU failure to backend for bucketuuid / objectuuid : " + upload.getBucket().getBucketUuid() + "/" + upload.getObjectUuid(),
          e);

      // Remove metadata and return failure
      try {
        ObjectMetadataManagers.getInstance().transitionObjectToState(upload, ObjectState.deleting);
      } catch (Exception ex) {
        LOG.warn("Failed to mark failed object entity in deleting state on failure rollback. Will be cleaned later.", e);
      }

      throw new InternalErrorException(upload.getObjectKey());
    }

    try {
      // Update metadata to "mpu-pending". Retry as necessary. Just used the entity itself for holding the timestamp and uploadId. Those
      // will be set and persisted in this call.
      return ObjectMetadataManagers.getInstance().finalizeMultipartInit(upload, upload.getObjectModifiedTimestamp(), upload.getUploadId());
    } catch (Exception e) {
      LOG.warn("Failed to update object metadata for finalization. Failing InitiateMPU operation", e);
      throw new InternalErrorException(upload.getResourceFullName());
    }
  }

  /**
   * Create the named object part in metadata and on the backend.
   *
   * @return the ObjectEntity object representing the successfully created object
   */
  @Override
  public PartEntity createObjectPart(final ObjectStorageProviderClient provider, ObjectEntity mpuEntity, PartEntity entity,
      final InputStream content, User requestUser) throws S3Exception {
    // Initialize metadata for the object
    if (BucketState.extant.equals(entity.getBucket().getState())) {
      // Initialize the object metadata.
      try {
        entity = MpuPartMetadataManagers.getInstance().initiatePartCreation(entity);
      } catch (Exception e) {
        // Metadata failure
        LOG.error("Error initializing metadata for object creation: " + entity.getResourceFullName());
        InternalErrorException ex = new InternalErrorException(entity.getResourceFullName());
        ex.initCause(e);
        throw ex;
      }

    } else {
      throw new NoSuchBucketException(entity.getBucket().getBucketName());
    }

    final Date lastModified;
    final String etag;
    UploadPartResponseType response;

    try {
      final PartEntity uploadingObject = entity;
      final UploadPartType putRequest = new UploadPartType();
      putRequest.setBucket(uploadingObject.getBucket().getBucketUuid());
      putRequest.setKey(mpuEntity.getObjectUuid());
      putRequest.setUser(requestUser);
      putRequest.setContentLength(entity.getSize().toString());
      putRequest.setPartNumber( valueOf(entity.getPartNumber()));
      putRequest.setUploadId(entity.getUploadId());

      Callable<UploadPartResponseType> putCallable = new Callable<UploadPartResponseType>() {

        @Override
        public UploadPartResponseType call() throws Exception {
          LOG.trace("Putting data");
          UploadPartResponseType response = provider.uploadPart(putRequest, content);
          LOG.trace("Done with put. " + response.getStatusMessage());
          return response;
        }
      };

      // Send the data
      final FutureTask<UploadPartResponseType> putTask = new FutureTask<>(putCallable);
      PUT_OBJECT_SERVICE.execute(putTask);
      final long failTime = System.currentTimeMillis() + getPutTimeoutInMillis();
      final long checkIntervalSec = ObjectStorageProperties.OBJECT_CREATION_EXPIRATION_INTERVAL_SEC / 2;
      final AtomicReference<PartEntity> entityRef = new AtomicReference<>(uploadingObject);
      Callable updateTimeout = new Callable() {
        @Override
        public Object call() throws Exception {
          PartEntity tmp = entityRef.get();
          try {
            entityRef.getAndSet(MpuPartMetadataManagers.getInstance().updateCreationTimeout(tmp));
          } catch (Exception ex) {
            LOG.warn("Could not update the creation expiration time for PartUUID " + tmp.getPartUuid() + " Will retry next interval", ex);
          }
          return entityRef.get();
        }
      };

      response = waitForCompletion(putTask, uploadingObject.getPartUuid(), updateTimeout, failTime, checkIntervalSec);
      entity = entityRef.get(); // Get the latest if it was updated
      // lastModified = response.getLastModified();
      lastModified = new Date();
      etag = response.getEtag();

    } catch (Exception e) {
      LOG.error("Data PUT failure to backend for bucketuuid / objectuuid : " + entity.getBucket().getBucketUuid() + "/" + entity.getPartUuid(), e);

      // Remove metadata and return failure
      try {
        MpuPartMetadataManagers.getInstance().transitionPartToState(entity, ObjectState.deleting);
      } catch (Exception ex) {
        LOG.error("Failed to mark failed object entity in deleting state on failure rollback. Will be cleaned later.", e);
      }

      // ObjectMetadataManagers.getInstance().delete(objectEntity);
      throw new InternalErrorException(entity.getObjectKey());
    }

    try {
      // Update metadata to "extant". Retry as necessary
      return MpuPartMetadataManagers.getInstance().finalizeCreation(entity, lastModified, etag);
    } catch (Exception e) {

      // Return failure to user, let normal cleanup handle this case since we can't update the metadata
      LOG.error("Failed to update object metadata for finalization. Failing PUT operation", e);
      throw new InternalErrorException(entity.getResourceFullName());
    }
  }

  /**
   * Commits a Multipart Upload into an extant object entity.
   *
   * @param provider
   * @param mpuEntity the ObjectEntity that is the upload parent record, as supplied by the ObjectMetadataManager.lookupUpload()
   * @param requestUser
   * @return
   * @throws S3Exception
   */
  @Override
  public ObjectEntity completeMultipartUpload(final ObjectStorageProviderClient provider, ObjectEntity mpuEntity, ArrayList<Part> partList,
      User requestUser) throws S3Exception {
    try {
      final CompleteMultipartUploadType commitRequest = new CompleteMultipartUploadType();
      commitRequest.setParts(partList);
      commitRequest.setBucket(mpuEntity.getBucket().getBucketUuid());
      commitRequest.setKey(mpuEntity.getObjectUuid());
      commitRequest.setUploadId(mpuEntity.getUploadId());

      // TODO: this is broken, need the exact set of parts used, not all parts
      long fullSize =
          MpuPartMetadataManagers.getInstance().processPartListAndGetSize(partList,
              MpuPartMetadataManagers.getInstance().getParts(mpuEntity.getBucket(), mpuEntity.getObjectKey(), mpuEntity.getUploadId()));
      mpuEntity.setSize(fullSize);
      Callable<CompleteMultipartUploadResponseType> completeCallable = new Callable<CompleteMultipartUploadResponseType>() {

        @Override
        public CompleteMultipartUploadResponseType call() throws Exception {
          CompleteMultipartUploadResponseType response = provider.completeMultipartUpload(commitRequest);
          LOG.debug("Done with multipart upload. " + response.getStatusMessage());
          return response;
        }
      };

      // Send the data
      final FutureTask<CompleteMultipartUploadResponseType> completeTask = new FutureTask<>(completeCallable);
      PUT_OBJECT_SERVICE.execute(completeTask);
      final long failTime = System.currentTimeMillis() + getPutTimeoutInMillis();
      final long checkIntervalSec = 60;
      CompleteMultipartUploadResponseType response =
          waitForMultipartCompletion(completeTask, commitRequest.getUploadId(), commitRequest.getCorrelationId(), failTime, checkIntervalSec);
      mpuEntity.seteTag(response.getEtag());

      ObjectEntity completedEntity = ObjectMetadataManagers.getInstance().finalizeCreation(mpuEntity, new Date(), mpuEntity.geteTag());

      // all okay, delete all parts
      /*
       * This is handled in the object state transition now. All done in one transaction. try {
       * MpuPartMetadataManagers.getInstance().removeParts(completedEntity.getBucket(), completedEntity.getUploadId()); } catch (Exception e) { throw
       * new InternalErrorException("Could not remove parts for: " + mpuEntity.getUploadId()); }
       */
      return completedEntity;
    } catch (S3Exception e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Failed commit of multipart upload " + mpuEntity.getUploadId(), e);
      InternalErrorException ex = new InternalErrorException(mpuEntity.getUploadId());
      ex.initCause(e);
      throw ex;
    }
  }

  /**
   * Flushes the mulitpart upload and all artifacts that are not committed.
   *
   * @param entity ObjectEntity record for object to delete
   */
  @Override
  public void flushMultipartUpload(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception {
    try {
      MpuPartMetadataManagers.getInstance().removeParts(entity.getBucket(), entity.getUploadId());
    } catch (Exception e) {
      LOG.warn("Error removing non-committed parts", e);
      InternalErrorException ex = new InternalErrorException();
      ex.initCause(e);
      throw ex;
    }

  }

}
