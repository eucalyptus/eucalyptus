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

package com.eucalyptus.objectstorage.entities

import com.eucalyptus.auth.Accounts
import com.eucalyptus.objectstorage.BucketState
import com.eucalyptus.objectstorage.UnitTestSupport
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.config.ConfigurationCache
import com.eucalyptus.storage.msgs.s3.BucketListEntry
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

@CompileStatic
class BucketTest  {

  @BeforeClass
  static void setUp() {
    UnitTestSupport.setupAuthPersistenceContext();
  }

  @AfterClass
  static void tearDown() {
    UnitTestSupport.tearDownAuthPersistenceContext();
  }
  @Test
  void testGetInitializedBucket() {
    Bucket b = Bucket.getInitializedBucket('bucket1', 'canonicalid1', 'displayname1', 'userid1', '{canonicalid1:8}','')
    assert(b.getBucketName() == 'bucket1')
    assert(b.getOwnerCanonicalId() == 'canonicalid1')
    assert(b.getOwnerIamUserId() == 'userid1')
    assert(b.getAcl() == '{canonicalid1:8}')
  }

  static long getBucketCreationWaitIntervalMillis () {
    return 5 * 1000l; //for testing use 5 seconds
  }
  @Test
  void testStateStillValid() {
    Bucket b = new Bucket()
    b.setState(BucketState.creating)
    int timeoutSec = (int)(getBucketCreationWaitIntervalMillis() / 1000l);

    b.setLastUpdateTimestamp(new Date(System.currentTimeMillis()  - (getBucketCreationWaitIntervalMillis() + 1)))
    assert(!b.stateStillValid(timeoutSec))

    b.setLastUpdateTimestamp(new Date())
    assert(b.stateStillValid(timeoutSec ))

    b.setLastUpdateTimestamp(new Date(System.currentTimeMillis() - (getBucketCreationWaitIntervalMillis() - 100)))
    assert(b.stateStillValid(timeoutSec))

    //Extant state is alwasy valid
    b.setState(BucketState.extant)
    assert(b.stateStillValid(timeoutSec))

    //Extant state is alwasy valid
    b.setState(BucketState.extant)
    b.setLastUpdateTimestamp(new Date())
    assert(b.stateStillValid(timeoutSec))

    //Deleting state is always valid
    b.setState(BucketState.deleting)
    b.setLastUpdateTimestamp(new Date(System.currentTimeMillis() - (getBucketCreationWaitIntervalMillis() + 10000)))
    assert(b.stateStillValid(timeoutSec))

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
