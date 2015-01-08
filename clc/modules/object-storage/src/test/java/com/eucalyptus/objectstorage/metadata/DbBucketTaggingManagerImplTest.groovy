/*
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
 */

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
