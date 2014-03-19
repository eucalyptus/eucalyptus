package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.User
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
        Account accnt = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().getAt(0))
        TEST_CANONICALUSER_1 = new CanonicalUser(accnt.getCanonicalId(), accnt.getUsers().get(0).getInfo("email"))

        accnt = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().getAt(1))
        TEST_CANONICALUSER_2 = new CanonicalUser(accnt.getCanonicalId(), accnt.getUsers().get(0).getInfo("email"))

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
        CanonicalUser owner = new CanonicalUser(Accounts.lookupAccountByName(accountName).getCanonicalId(), Accounts.lookupAccountByName(accountName).getName())
        acp.setOwner(owner)
        acp.setAccessControlList(new AccessControlList())
        acp.getAccessControlList().setGrants(new ArrayList<Grant>())
        acp.getAccessControlList().getGrants().add(new Grant(new Grantee(owner), ObjectStorageProperties.Permission.FULL_CONTROL.toString()))
        Bucket b = Bucket.getInitializedBucket(name, UnitTestSupport.getUsersByAccountName(accountName).first(), acp, "")
        return mgr.transitionBucketToState(b, BucketState.creating)

    }

    public static List<ObjectEntity> createNObjects(
            final ObjectMetadataManager objMgr, int count,
            final Bucket bucket, String keyPrefix, long contentLength, final User usr) {
        def objectEntities = []
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