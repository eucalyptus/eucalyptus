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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Predicate;

public class BucketFactoryImpl implements BucketFactory {
  private static final Logger LOG = Logger.getLogger(BucketFactoryImpl.class);

  @Override
  public Bucket createBucket(@Nonnull ObjectStorageProviderClient backendProvider, @Nonnull Bucket bucket, @Nullable String correlationId,
      @Nullable User requestUser) throws S3Exception {

    // Ensure not null for logging. CorrelationId is not required, just used to track user requests.
    if (correlationId == null) {
      correlationId = "unknown";
    }

    // Initiate metadata creation. State = 'creating'
    try {
      // The bucket must either be created in 'creating' state or exception thrown.
      bucket = BucketMetadataManagers.getInstance().transitionBucketToState(bucket, BucketState.creating);
    } catch (IllegalResourceStateException e) {
      throw new BucketAlreadyExistsException(bucket.getBucketName());
    } catch (MetadataOperationFailureException | ConstraintViolationException e) {
      throw new BucketAlreadyExistsException(bucket.getBucketName());
    } catch (Exception e) {
      // Failed metadata operation, not an expected error
      LOG.error("Error initiating bucket creation in metadata. Failing operation", e);
      InternalErrorException ex = new InternalErrorException(bucket.getBucketName());
      ex.initCause(e);
      throw ex;
    }

    if (bucket == null) {
      LOG.error("CorrelationId: " + correlationId
          + "Unexpected internal error. Got null bucket when not expected. Cannot continue bucket creation for " + bucket.getBucketName());
      throw new InternalErrorException();
    }

    if (BucketState.creating.equals(bucket.getState())) {
      // Do backend operation
      CreateBucketResponseType backendResponse = null;
      try {
        CreateBucketType request = new CreateBucketType();
        request.setAccessControlList(new AccessControlList());
        request.setBucket(bucket.getBucketUuid());
        request.setUser(requestUser);
        backendResponse = backendProvider.createBucket(request);

        // Finalize bucket state. Set State = 'extant'
        return BucketMetadataManagers.getInstance().transitionBucketToState(bucket, BucketState.extant);
      } catch (EucalyptusCloudException e) {
        LOG.error("Bucket creation failed due to error response from backend.", e);
        if (e instanceof S3Exception) {
          // TODO: need to translate this error to ensure it is not raw backend response
          LOG.error("Error creating bucket " + bucket.getBucketName(), e);
          throw (S3Exception) e;
        } else {
          InternalErrorException ex = new InternalErrorException();
          ex.initCause(e);
          throw ex;
        }
      } catch (Exception e) {
        LOG.warn("Unknown exception caused failure of CreateBucket for bucket " + bucket.getBucketName(), e);
        InternalErrorException ex = new InternalErrorException(bucket.getBucketName());
        ex.initCause(e);
        throw ex;
      }
    } else {
      // In some other state that we don't expect. Bail, it already exists
      throw new BucketAlreadyExistsException(bucket.getBucketName());
    }
  }

  @Override
  public void deleteBucket(@Nonnull final ObjectStorageProviderClient backendProvider, @Nonnull Bucket bucketToDelete,
      @Nullable final String correlationId, @Nullable User requestUser) throws S3Exception {
    Bucket deletingBucket;
    try {
      deletingBucket = BucketMetadataManagers.getInstance().transitionBucketToState(bucketToDelete, BucketState.deleting);
    } catch (IllegalResourceStateException e) {
      LOG.trace("CorrelationId: " + correlationId + " Unexpected resource state on delete update.", e);
      throw e;
    } catch (MetadataOperationFailureException e) {
      LOG.trace("CorrelationId: " + correlationId + " Could not transition bucket " + bucketToDelete.toString() + " to 'deleting' state.", e);
      throw e;
    }

    /*
     * Remove the entities that may refer to the bucket record. Parts cannot be explicitly deleted in S3, they are removed when the bucket is deleted.
     * So we flush the metadata here
     */
    try {
      // Remove all pending uploads
      ObjectMetadataManagers.getInstance().flushUploads(bucketToDelete);

      // Delete all parts and pending MPU uploads
      MpuPartMetadataManagers.getInstance().flushAllParts(bucketToDelete);

      // Delete the lifecycle configuration
      BucketLifecycleManagers.getInstance().deleteLifecycleRules(bucketToDelete.getBucketUuid());

      // Delete the bucket tagging
      BucketTaggingManagers.getInstance().deleteBucketTagging(bucketToDelete.getBucketUuid());
    } catch (Exception e) {
      LOG.warn("Error flushing MPU parts during bucket deletion operation", e);
      throw new InternalErrorException(e);
    }

    Predicate<Bucket> deleteBucket = new Predicate<Bucket>() {
      public boolean apply(Bucket bucket) {
        DeleteBucketResponseType response;
        DeleteBucketType deleteRequest = new DeleteBucketType();
        deleteRequest.setBucket(bucket.getBucketUuid());
        try {
          backendProvider.deleteBucket(deleteRequest); // should throw an exception on any failure
        } catch (NoSuchEntityException | NoSuchBucketException e) {
          // Ok, fall through
        } catch (S3Exception e) {
          if (!HttpResponseStatus.NOT_FOUND.equals(e.getStatus())) {
            LOG.warn("Got error during bucket cleanup. Will retry", e);
            return false;
          }
        }

        try {
          BucketMetadataManagers.getInstance().deleteBucketMetadata(bucket);
          return true;
        } catch (Exception e) {
          LOG.warn("Error removing bucket metadata for bucket " + bucket.getBucketUuid() + " Will retry later", e);
        }
        return false;
      }
    };

    try {
      Entities.asTransaction(Bucket.class, deleteBucket).apply(deletingBucket);
    } catch (Exception e) {
      try {
        Bucket foundBucket = BucketMetadataManagers.getInstance().lookupBucket(bucketToDelete.getBucketName());
        LOG.trace("CorrelationId: " + correlationId + " Error deleting bucket " + bucketToDelete.toString(), e);
        throw new InternalErrorException(bucketToDelete.getBucketName());
      } catch (Exception ex) {
        // Bucket not found. Success!
      }
    }
  }
}
