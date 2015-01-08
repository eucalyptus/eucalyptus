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


import com.eucalyptus.objectstorage.entities.BucketTags;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.storage.msgs.s3.BucketTag;

import java.util.List;

public interface BucketTaggingManager {

  public void addBucketTagging( List<BucketTag> bucketTags, String bucketUuid ) throws ObjectStorageException;

  public void deleteBucketTagging( String bucketUuid ) throws ObjectStorageException;

  public List<BucketTags> getBucketTagging( String bucketUuid ) throws NoSuchEntityException, ObjectStorageException;

}
