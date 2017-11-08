/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.BucketTaggingManagers;
import com.eucalyptus.objectstorage.entities.BucketTags;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.storage.msgs.s3.BucketTag;

public class DbBucketTaggingManagerImpl implements BucketTaggingManager {

  private static Logger LOG = Logger.getLogger(DbBucketTaggingManagerImpl.class);

  @Override
  public void addBucketTagging(@Nonnull List<BucketTag> tags, @Nonnull String bucketUuid) throws ObjectStorageException {

    try (TransactionResource tx = Entities.transactionFor(BucketTags.class)) {
      BucketTaggingManagers.getInstance().deleteBucketTagging(bucketUuid);

      for (BucketTag bucketTag : tags) {
        BucketTags entity = new BucketTags();
        entity.setBucketUuid(bucketUuid);
        entity.setKey(bucketTag.getKey());
        entity.setValue(bucketTag.getValue());
        Entities.merge(entity);
      }
      tx.commit();

    } catch (Exception e) {
      LOG.trace("Error in setting entity for tagging to database" + e);
      throw new ObjectStorageException("InternalServerError", "An exception was caught while adding TagSet for bucket - " + bucketUuid, "Bucket",
          bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void deleteBucketTagging(@Nonnull String bucketUuid) throws ObjectStorageException {
    try (TransactionResource tx = Entities.transactionFor(BucketTags.class)) {

      Map<String, String> parameters = new HashMap<>();
      parameters.put("bucketUuid", bucketUuid);

      Entities.deleteAllMatching(BucketTags.class, "WHERE bucketUuid = :bucketUuid", parameters);
      tx.commit();
    } catch (Exception e) {
      LOG.trace("Error in deleting entity for tagging to database" + e);
      throw new ObjectStorageException("InternalServerError", "An exception was caught while deleting TagSet for bucket - " + bucketUuid, "Bucket",
          bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public List<BucketTags> getBucketTagging(@Nonnull String bucketUuid) throws NoSuchEntityException, ObjectStorageException {

    try (final TransactionResource tx = Entities.transactionFor(BucketTags.class)) {
      List<BucketTags> resultTags = Entities.query(new BucketTags().withUuid(bucketUuid));
      tx.commit();

      return resultTags;
    } catch (Exception e) {
      LOG.trace("Error in getting entity for tagging to database: " + e);
      throw new ObjectStorageException("InternalServerError", "An exception was caught while deleting TagSet for bucket - " + bucketUuid, "Bucket",
          bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
