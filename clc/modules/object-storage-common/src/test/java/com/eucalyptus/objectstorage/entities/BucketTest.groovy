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
 ************************************************************************/

package com.eucalyptus.objectstorage.entities

import com.eucalyptus.objectstorage.BucketState
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.BucketListEntry
import groovy.transform.CompileStatic
import org.junit.Test

@CompileStatic
class BucketTest  {

    @Test
    void testGetInitializedBucket() {
        Bucket b = Bucket.getInitializedBucket('bucket1', 'canonicalid1', 'displayname1', 'userid1', '{canonicalid1:8}','')
        assert(b.getBucketName() == 'bucket1')
        assert(b.getOwnerCanonicalId() == 'canonicalid1')
        assert(b.getOwnerIamUserId() == 'userid1')
        assert(b.getAcl() == '{canonicalid1:8}')
    }

    @Test
    void testStateStillValid() {
        Bucket b = new Bucket()
        b.setState(BucketState.creating)

        b.setLastUpdateTimestamp(new Date(System.currentTimeMillis()  - 1000*(ObjectStorageGlobalConfiguration.bucket_creation_wait_interval_seconds + 1)))
        assert(!b.stateStillValid())

        b.setLastUpdateTimestamp(new Date())
        assert(b.stateStillValid())

        b.setLastUpdateTimestamp(new Date(System.currentTimeMillis() - 1000*(ObjectStorageGlobalConfiguration.bucket_creation_wait_interval_seconds - 100)))
        assert(b.stateStillValid())

        //Extant state is alwasy valid
        b.setState(BucketState.extant)
        assert(b.stateStillValid())

        //Extant state is alwasy valid
        b.setState(BucketState.extant)
        b.setLastUpdateTimestamp(new Date())
        assert(b.stateStillValid())

        //Deleting state is always valid
        b.setState(BucketState.deleting)
        b.setLastUpdateTimestamp(new Date(System.currentTimeMillis() - 1000*(ObjectStorageGlobalConfiguration.bucket_creation_wait_interval_seconds + 10000)))
        assert(b.stateStillValid())

    }

    @Test
    void testSearchBucketExampleWithUuid() {
        Bucket b = new Bucket().withUuid("fake-uuid");
        assert(b.getBucketName() == null)
        assert(b.getBucketUuid() == "fake-uuid")
    }

    @Test
    void testHasLoggingPerms() {
        int logDeliveryBitmap = S3AccessControlledEntity.BitmapGrant.add(ObjectStorageProperties.Permission.WRITE,S3AccessControlledEntity.BitmapGrant.translateToBitmap(ObjectStorageProperties.Permission.READ_ACP))
        int fullControlBitmap = S3AccessControlledEntity.BitmapGrant.translateToBitmap(ObjectStorageProperties.Permission.FULL_CONTROL)
        def aclString = '{"canonicalid1":'+fullControlBitmap+',"http://acs.amazonaws.com/groups/s3/LogDelivery":' + logDeliveryBitmap + '}';
        Bucket b = Bucket.getInitializedBucket('bucket1', 'canonicalid1', 'displayname1', 'userid1', aclString ,'')
        assert(b.getBucketName() == 'bucket1')
        assert(b.getOwnerCanonicalId() == 'canonicalid1')
        assert(b.getOwnerIamUserId() == 'userid1')
        assert(b.getAcl() == aclString)
        assert(b.hasLoggingPerms())
    }

    @Test
    void testIsOwnedBy() {
        Bucket b = Bucket.getInitializedBucket('bucket1', 'canonicalid1', 'displayname1', 'userid1', '{canonicalid1:15}','')
        assert(b.getBucketName() == 'bucket1')
        assert(b.getOwnerCanonicalId() == 'canonicalid1')
        assert(b.getOwnerIamUserId() == 'userid1')
        assert(b.getAcl() == '{canonicalid1:15}')

        assert(b.isOwnedBy('canonicalid1'))
    }

    @Test
    void testGenerateVersionId() {
        Bucket b = new Bucket()
        b.setVersioning(ObjectStorageProperties.VersioningStatus.Disabled)
        assert(b.generateObjectVersionId().equals(ObjectStorageProperties.NULL_VERSION_ID))

        b.setVersioning(ObjectStorageProperties.VersioningStatus.Enabled)
        assert(!b.generateObjectVersionId().equals(ObjectStorageProperties.NULL_VERSION_ID))
        assert(b.generateObjectVersionId().size() > 0)

        b.setVersioning(ObjectStorageProperties.VersioningStatus.Suspended)
        assert(b.generateObjectVersionId().equals(ObjectStorageProperties.NULL_VERSION_ID))
    }

    @Test
    void testToBucketListEntry() {
        Bucket b = Bucket.getInitializedBucket('bucket1', 'canonicalid1', 'displayname1', 'userid1', '{canonicalid1:8}','')
        assert(b.getBucketName() == 'bucket1')
        assert(b.getOwnerCanonicalId() == 'canonicalid1')
        assert(b.getOwnerIamUserId() == 'userid1')
        assert(b.getAcl() == '{canonicalid1:8}')

        BucketListEntry entry = b.toBucketListEntry()
        assert(entry != null)
        assert(entry.getCreationDate() == b.getCreationTimestamp())
        assert(entry.getName() == b.getBucketName())
    }
}
