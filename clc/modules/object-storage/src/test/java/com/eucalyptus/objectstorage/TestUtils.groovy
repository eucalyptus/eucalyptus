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

package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.AccountIdentifiers
import com.eucalyptus.auth.principal.UserPrincipal
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.entities.ObjectEntity
import com.eucalyptus.objectstorage.metadata.BucketMetadataManager
import com.eucalyptus.objectstorage.metadata.ObjectMetadataManager
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.AccessControlList
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.CanonicalUser
import com.eucalyptus.storage.msgs.s3.Grant
import com.eucalyptus.storage.msgs.s3.Grantee
import groovy.transform.CompileStatic

/**
 * Created by zhill on 1/31/14.
 */
@CompileStatic
public class TestUtils {
  static CanonicalUser TEST_CANONICALUSER_1
  static CanonicalUser TEST_CANONICALUSER_2
  static AccessControlList TEST_ACCOUNT1_PRIVATE_ACL = new AccessControlList()
  static AccessControlList TEST_ACCOUNT2_PRIVATE_ACL = new AccessControlList()
  static Grant TEST_ACCOUNT1_FULLCONTROL_GRANT;
  static Grant TEST_ACCOUNT2_FULLCONTROL_GRANT;

  public static initTestAccountsAndAcls() {
    AccountIdentifiers accnt1 = Accounts.lookupAccountIdentifiersByAlias(UnitTestSupport.getTestAccounts().getAt(0))
    TEST_CANONICALUSER_1 = new CanonicalUser(accnt1.getCanonicalId(), accnt1.accountAlias)

    AccountIdentifiers accnt2 = Accounts.lookupAccountIdentifiersByAlias(UnitTestSupport.getTestAccounts().getAt(1))
    TEST_CANONICALUSER_2 = new CanonicalUser(accnt2.getCanonicalId(), accnt2.accountAlias)

    TEST_ACCOUNT1_FULLCONTROL_GRANT = new Grant(new Grantee(TEST_CANONICALUSER_1), ObjectStorageProperties.Permission.FULL_CONTROL.toString());
    TEST_ACCOUNT1_PRIVATE_ACL.setGrants(new ArrayList<Grant>());
    TEST_ACCOUNT1_PRIVATE_ACL.getGrants().add(TEST_ACCOUNT1_FULLCONTROL_GRANT);

    TEST_ACCOUNT2_FULLCONTROL_GRANT = new Grant(new Grantee(TEST_CANONICALUSER_2), ObjectStorageProperties.Permission.FULL_CONTROL.toString());
    TEST_ACCOUNT2_PRIVATE_ACL.setGrants(new ArrayList<Grant>());
    TEST_ACCOUNT2_PRIVATE_ACL.getGrants().add(TEST_ACCOUNT2_FULLCONTROL_GRANT);
  }

  public static Bucket createTestBucket(BucketMetadataManager mgr, String name) {
    createTestBucket(mgr, name, null)
  }

  public static Bucket createTestBucket(BucketMetadataManager mgr, String name, String accountName) {
    if (accountName == null) {
      accountName = UnitTestSupport.getTestAccounts().first()
    }
    Bucket b = initializeBucket(mgr, name, accountName)
    return BucketMetadataManagers.getInstance().transitionBucketToState(b, BucketState.extant)
  }

  public static Bucket initializeBucket(BucketMetadataManager mgr, String name, String accountName) {
    AccessControlPolicy acp = new AccessControlPolicy()
    AccountIdentifiers accountIdentifiers = Accounts.lookupAccountIdentifiersByAlias(accountName)
    CanonicalUser owner = new CanonicalUser(accountIdentifiers.getCanonicalId(), accountIdentifiers.getAccountAlias())
    acp.setOwner(owner)
    acp.setAccessControlList(new AccessControlList())
    acp.getAccessControlList().setGrants(new ArrayList<Grant>())
    acp.getAccessControlList().getGrants().add(new Grant(new Grantee(owner), ObjectStorageProperties.Permission.FULL_CONTROL.toString()))
    Bucket b = Bucket.getInitializedBucket(name, UnitTestSupport.getUsersByAccountName(accountName).first(), acp, "")
    return mgr.transitionBucketToState(b, BucketState.creating)
  }

  public static List<ObjectEntity> createNObjects(
      final ObjectMetadataManager objMgr, int count,
      final Bucket bucket, String keyPrefix, long contentLength, final UserPrincipal usr) {
    List<ObjectEntity> objectEntities = []
    for (int i = 0; i < count; i++) {
      ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, keyPrefix + i, contentLength, usr)
      entity = objMgr.initiateCreation(entity);
      assert (objMgr.lookupObjectsInState(bucket, keyPrefix + i, entity.getVersionId(), ObjectState.creating).first().equals(entity))
      entity = objMgr.finalizeCreation(entity, new Date(), UUID.randomUUID().toString())
      assert (objMgr.lookupObjectsInState(bucket, keyPrefix + i, entity.getVersionId(), ObjectState.extant).first().equals(entity))
      objectEntities += entity
    }
    return objectEntities
  }
}