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

package com.eucalyptus.objectstorage.metadata

import com.eucalyptus.objectstorage.BucketTaggingManagers
import com.eucalyptus.objectstorage.UnitTestSupport
import com.eucalyptus.objectstorage.entities.BucketTags
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException
import com.eucalyptus.storage.msgs.s3.BucketTag
import com.google.common.collect.Lists
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Created by zhill on 9/26/14.
 */
class DbBucketTaggingManagerImplTest {
  private BucketTaggingManager mgr = BucketTaggingManagers.getInstance();

  @BeforeClass
  public static void testSetup() {
    UnitTestSupport.setupOsgPersistenceContext();
  }

  @AfterClass
  public static void testTeardown() {
    UnitTestSupport.tearDownOsgPersistenceContext();
  }

  private void cleanTags(String bucketName) throws Exception {
    mgr.deleteBucketTagging(bucketName)
    try {
      List<BucketTags> tags = mgr.getBucketTagging(bucketName);
      assertTrue("Found tags after deletion. Should all be removed", tags == null || tags.size() == 0);
    } catch(ObjectStorageException e) {
      //ok, expected error on delete if already removed
    }
  }

  @Test
  void testAddBucketTagging() {
    String bucketId = "testbucketid";
    List<BucketTag> tagList = Lists.newArrayList()
    BucketTag tag1 = new BucketTag("key1","value1")
    BucketTag tag2 = new BucketTag("key2","value2")
    tagList.add(tag1)
    tagList.add(tag2)
    mgr.addBucketTagging(tagList, bucketId)

    List<BucketTags> foundTags = mgr.getBucketTagging(bucketId)
    assert(foundTags != null)
    assert(foundTags.size() == 2)
    assert(foundTags.get(0).bucketUuid == bucketId)
    foundTags.sort { a, b -> a.key <=> b.key }
    assert(foundTags.get(0).key == 'key1' && foundTags.get(0).value == 'value1')
    assert(foundTags.get(1).key == 'key2' && foundTags.get(1).value == 'value2')

    cleanTags(bucketId)
  }

  @Test
  void testDeleteBucketTagging() {
    String bucketId = "testbucketid";
    List<BucketTag> tagList = Lists.newArrayList()
    BucketTag tag1 = new BucketTag("key1", "value1")
    BucketTag tag2 = new BucketTag("key2", "value2")
    tagList.add(tag1)
    tagList.add(tag2)
    mgr.addBucketTagging(tagList, bucketId)

    List<BucketTags> foundTags = mgr.getBucketTagging(bucketId)
    assert (foundTags != null)
    assert (foundTags.size() == 2)

    mgr.deleteBucketTagging(bucketId)
    List<BucketTags> unexpectedTags = mgr.getBucketTagging(bucketId)

    cleanTags(bucketId)
  }
}
