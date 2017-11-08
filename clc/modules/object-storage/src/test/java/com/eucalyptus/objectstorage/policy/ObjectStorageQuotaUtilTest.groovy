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

package com.eucalyptus.objectstorage.policy

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.AccountIdentifiers
import com.eucalyptus.auth.principal.UserPrincipal
import com.eucalyptus.objectstorage.BucketMetadataManagers
import com.eucalyptus.objectstorage.BucketState
import com.eucalyptus.objectstorage.ObjectMetadataManagers
import com.eucalyptus.objectstorage.ObjectState
import com.eucalyptus.objectstorage.UnitTestSupport
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.entities.ObjectEntity
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by zhill on 3/13/14.
 */
@CompileStatic
public class ObjectStorageQuotaUtilTest {
  static List<Bucket> buckets = Lists.newArrayList()
  static List<ObjectEntity> objects = Lists.newArrayList()
  static int bucketCount = 3
  static int objectCount = 5
  static int size = 1024 * 1024
  static AccountIdentifiers account1
  static AccountIdentifiers account2
  static UserPrincipal a1u1
  static UserPrincipal a1u2
  static UserPrincipal a2u1
  static UserPrincipal a2u2

  private static void initMetaData() {
    def name = "bucket"
    account1 = Accounts.lookupAccountIdentifiersByAlias(UnitTestSupport.getTestAccounts().first())
    a1u1 = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    a1u2 = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).getAt(1))

    account2 = Accounts.lookupAccountIdentifiersByAlias(UnitTestSupport.getTestAccounts().getAt(1))
    a2u1 = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().getAt(1)).first())
    a2u2 = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().getAt(1)).getAt(1))

    def acl = ""
    def location = ""
    def b

    for (int i = 0; i < bucketCount ; i++) {
      b = Bucket.getInitializedBucket(name + i, account1.getCanonicalId(), account1.getAccountAlias(), a1u1.getUserId(), acl, location)
      b = BucketMetadataManagers.getInstance().transitionBucketToState(b, BucketState.creating)
      b = BucketMetadataManagers.getInstance().transitionBucketToState(b, BucketState.extant)
      buckets.add(b)

      def obj
      def key = 'key'
      for (int j = 0; j < objectCount ; j++) {
        obj = ObjectEntity.newInitializedForCreate(b, key + j, size, a1u1)
        obj = ObjectMetadataManagers.getInstance().transitionObjectToState(obj, ObjectState.creating)
        obj.setObjectModifiedTimestamp(new Date())
        obj.seteTag("etag" + j)
        obj = ObjectMetadataManagers.getInstance().transitionObjectToState(obj, ObjectState.extant)
        objects.add(obj)
      }
    }
  }

  @BeforeClass
  public static void setup() {
    UnitTestSupport.setupAuthPersistenceContext()
    UnitTestSupport.setupOsgPersistenceContext()
    UnitTestSupport.initializeAuth(2, 2)

    initMetaData()
  }

  @AfterClass
  public static void teardown() {
    UnitTestSupport.tearDownOsgPersistenceContext()
    UnitTestSupport.tearDownAuthPersistenceContext()
  }

  @Before
  public void preTest() {}

  @After
  public void postTest() {}

  @Test
  public void testCountBucketsByAccount() throws Exception {
    assert(ObjectStorageQuotaUtil.countBucketsByAccount(account1.getAccountNumber()) == bucketCount)
    assert(ObjectStorageQuotaUtil.countBucketsByAccount(account2.getAccountNumber()) == 0)
  }

  @Test
  public void testCountBucketsByUser() throws Exception {
    assert(ObjectStorageQuotaUtil.countBucketsByUser(a1u1.getUserId()) == bucketCount)
    assert(ObjectStorageQuotaUtil.countBucketsByUser(a1u2.getUserId()) == 0)
    assert(ObjectStorageQuotaUtil.countBucketsByUser(a2u1.getUserId()) == 0)
    assert(ObjectStorageQuotaUtil.countBucketsByUser(a2u2.getUserId()) == 0)
  }

  @Test
  public void testCountBucketObjects() throws Exception {
    buckets.each {
      assert(ObjectStorageQuotaUtil.countBucketObjects(((Bucket)it).getBucketName()) == objectCount)
    }
  }

  @Test
  public void testGetBucketSize() throws Exception {
    buckets.each {
      assert(ObjectStorageQuotaUtil.getBucketSize(((Bucket)it).getBucketName()) == objectCount * size)
    }
  }

  @Test
  public void testGetTotalObjectsSizeByAccount() throws Exception {
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByAccount(account1.getAccountNumber()) ==  bucketCount * objectCount * size)
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByAccount(account2.getAccountNumber()) == 0)
  }

  @Test
  public void testGetTotalObjectsSizeByUser() throws Exception {
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a1u1.getUserId()) ==  bucketCount * objectCount * size)
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a1u2.getUserId()) == 0)
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a2u1.getUserId()) == 0)
    assert(ObjectStorageQuotaUtil.getTotalObjectsSizeByUser(a2u2.getUserId()) == 0)
  }

  @Test
  public void testGetTotalObjectSize() throws Exception {
    assert(ObjectStorageQuotaUtil.getTotalObjectSize() == bucketCount * objectCount * size)
  }
}
