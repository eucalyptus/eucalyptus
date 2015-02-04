/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
