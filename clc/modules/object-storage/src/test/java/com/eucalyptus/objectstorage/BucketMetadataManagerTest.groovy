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

package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.entities.Transactions
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity
import com.eucalyptus.objectstorage.metadata.BucketMetadataManager
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.AccessControlList
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.CanonicalUser
import com.eucalyptus.storage.msgs.s3.Grant
import com.eucalyptus.storage.msgs.s3.Grantee
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.fail;

@CompileStatic
public class BucketMetadataManagerTest {
    private static final Logger LOG = Logger.getLogger(BucketMetadataManagerTest.class);

    static BucketMetadataManager mgr = BucketMetadataManagers.getInstance();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        UnitTestSupport.setupOsgPersistenceContext()
        UnitTestSupport.setupAuthPersistenceContext()
        UnitTestSupport.initializeAuth(2, 2)
        TestUtils.initTestAccountsAndAcls()
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        UnitTestSupport.tearDownOsgPersistenceContext()
        UnitTestSupport.tearDownAuthPersistenceContext()
    }

    @Before
    public void setUp() throws Exception {
        mgr.start()
        UnitTestSupport.flushObjects()
        UnitTestSupport.flushBuckets()

    }

    @After
    public void tearDown() throws Exception {
        mgr.stop()
        UnitTestSupport.flushObjects()
        UnitTestSupport.flushBuckets()
    }

    @Test
    public void testBasicBucketCreateDeleteCycle() {
        def bucketName = 'testbucket'
        def location = ''
        def acp = new AccessControlPolicy()

        acp.setOwner(TestUtils.TEST_CANONICALUSER_1)
        acp.setAccessControlList(TestUtils.TEST_ACCOUNT1_PRIVATE_ACL)

        println 'Testing basic start-create operation'

        Bucket bucket = mgr.persistBucketInCreatingState(bucketName, acp, Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getUsers().get(0).getUserId(), location)

        assert (bucket.getBucketName().equals(bucketName))
        assert (bucket.getBucketUuid().length() > ('-' + bucketName).length())
        assert (bucket.getState().equals(BucketState.creating))

        println 'Initiating finalization of bucket creation'
        Bucket createdBucket = mgr.transitionBucketToState(bucket, BucketState.extant)
        assert (createdBucket != null)
        assert (createdBucket.getState().equals(BucketState.extant))
        assert (createdBucket.getCreationTimestamp() != null)
        assert (createdBucket.getBucketSize() == 0L)
        assert (createdBucket.getBucketUuid().length() > ('-' + bucketName).length())

        println 'Fetching bucket. Should return extant entity'
        Bucket fetchedBucket = mgr.lookupBucket(bucketName)
        assert (fetchedBucket != null)
        assert (fetchedBucket.getState().equals(BucketState.extant))

        println 'Starting delete operation on bucket'
        fetchedBucket = mgr.transitionBucketToState(fetchedBucket, BucketState.deleting)
        assert (fetchedBucket.getState().equals(BucketState.deleting))

        println 'Finishing delete operation on bucket'
        mgr.deleteBucketMetadata(fetchedBucket)
        try {
            fetchedBucket = mgr.lookupBucket(bucketName)
            println 'Error: got bucket record not expected. Should be deleted. Deleting now'
            Transactions.delete(bucket);
            fail('Deleted bucket record still found ' + fetchedBucket.getBucketUuid() + fetchedBucket.getState())
        } catch (Exception e) {
            println 'Got exception on deleted bucket as expected'
        }
    }

    /**
     * Tests concurrent creations of the same bucket name.
     * Expected outcome:
     */
    @Test
    public void testConcurrentBucketCreateDeleteCycle() {
        def bucketName = 'testbucket'
        def location = ''
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TestUtils.TEST_CANONICALUSER_1)
        acp.setAccessControlList(TestUtils.TEST_ACCOUNT1_PRIVATE_ACL)

        println 'Testing basic start-create operation without pre-built uuid'
        Bucket bucket = mgr.persistBucketInCreatingState(bucketName, acp, Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getUsers().get(0).getUserId(), location)

        assert (bucket.getBucketName().equals(bucketName))
        assert (bucket.getBucketUuid() != null)
        assert (bucket.getState().equals(BucketState.creating))
        println 'Got bucket uuid = ' + bucket.getBucketUuid()

        println 'Initiating creation of bucket with same name'
        try {
            Bucket bucket2 = mgr.persistBucketInCreatingState(bucketName, acp, Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getUsers().get(0).getUserId(), location)
            println 'Error: should have failed 2nd create due to name conflict'
            fail('2nd bucket should fail $bucket2')
        } catch (Exception e) {
            println 'Caught expected exception ' + e.getMessage()
        }

        Bucket createdBucket = mgr.transitionBucketToState(bucket, BucketState.extant)
        assert (createdBucket != null)
        assert (createdBucket.getState().equals(BucketState.extant))
        assert (createdBucket.getCreationTimestamp() != null)
        assert (createdBucket.getBucketSize() == 0L)
        assert (createdBucket.getBucketUuid() != null)

        println 'Fetching bucket. Should return extant entity'
        Bucket fetchedBucket = mgr.lookupBucket(bucketName)
        assert (fetchedBucket != null)
        assert (fetchedBucket.getState().equals(BucketState.extant))

        mgr.transitionBucketToState(fetchedBucket, BucketState.deleting);
        mgr.deleteBucketMetadata(fetchedBucket)

        try {
            Bucket b = mgr.lookupBucket(bucketName)
            fail('Found record for bucket that should be deleted')
        } catch (Exception e) {
            println 'Caught expected exception on deleted bucket name ' + e.getMessage()
        }
    }

    @Test
    public void testConcurrentBucketCreateAndExtantCycle() {
        def bucketName = 'testbucket'
        def location = ''
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TestUtils.TEST_CANONICALUSER_1)
        acp.setAccessControlList(TestUtils.TEST_ACCOUNT1_PRIVATE_ACL)

        println 'Testing basic start-create operation without pre-built uuid'
        Bucket bucket = mgr.persistBucketInCreatingState(bucketName, acp, Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getUsers().get(0).getUserId(), location)

        assert (bucket.getBucketName().equals(bucketName))
        assert (bucket.getBucketUuid() != null)
        assert (bucket.getState().equals(BucketState.creating))
        println 'Got bucket uuid = ' + bucket.getBucketUuid()

        bucket = mgr.transitionBucketToState(bucket, BucketState.extant);
        assert (bucket.getState().equals(BucketState.extant))

        println 'Initiating creation of bucket with same name'
        try {
            Bucket bucket2 = mgr.persistBucketInCreatingState(bucketName, acp, Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getUsers().get(0).getUserId(), location)
            println 'Error: should have failed 2nd create due to name conflict'
            fail('2nd bucket should fail $bucket2')
        } catch (Exception e) {
            println 'Caught expected exception $e'
        }

        //Should work, is idempotent
        Bucket createdBucket = mgr.transitionBucketToState(bucket, BucketState.extant)
        assert (createdBucket != null)
        assert (createdBucket.getState().equals(BucketState.extant))
        assert (createdBucket.getCreationTimestamp() != null)
        assert (createdBucket.getBucketSize() == 0L)
        assert (createdBucket.getBucketUuid() != null)

        println 'Fetching bucket. Should return extant entity'
        Bucket fetchedBucket = mgr.lookupBucket(bucketName)
        assert (fetchedBucket != null)
        assert (fetchedBucket.getState().equals(BucketState.extant))

        fetchedBucket = mgr.transitionBucketToState(fetchedBucket, BucketState.deleting);
        //Name must be nulled to free the name for use
        assert (fetchedBucket.getBucketName() == null)

        mgr.deleteBucketMetadata(fetchedBucket)
        try {
            Bucket b = mgr.lookupBucket(bucketName)
            fail('Found record for bucket that should be deleted')
        } catch (Exception e) {
            println 'Caught expected exception on deleted bucket name'
        }
    }

    @Test
    public void testGet() {
        String accountName = UnitTestSupport.getTestAccounts().first()
        Bucket b1 = TestUtils.createTestBucket(mgr, 'bucket1')

        def fetchedb = mgr.lookupBucket(b1.getBucketName())
        assert (fetchedb != null && fetchedb.getBucketName() == 'bucket1')

        fetchedb = mgr.transitionBucketToState(b1, BucketState.extant)
        fetchedb = mgr.lookupBucket(b1.getBucketName())
        assert (fetchedb != null && fetchedb.getBucketName() == 'bucket1')

        fetchedb = mgr.transitionBucketToState(b1, BucketState.deleting)
        try {
            fetchedb = null
            fetchedb = mgr.lookupBucket(b1.getBucketName())
            fail('Fetch should fail on bucket in deleting state')
        } catch (Exception e) {
            println 'Correctly caught exception on fetching a bucket in deleting state ' + e.getMessage()

            fetchedb = null
        }

        assert (fetchedb == null)

        Bucket b2 = initializeBucket('bucket1', accountName)
        Bucket b3 = initializeBucket('bucket2', accountName)

        assert (mgr.lookupBucket('bucket1') != null && mgr.lookupBucket('bucket2') != null)
        b2 = mgr.transitionBucketToState(b2, BucketState.extant)
        assert (mgr.lookupBucket('bucket1') != null && mgr.lookupBucket('bucket2') != null)
    }

    @Test
    public void testList() {
        int count = 10
        for (int i = 0; i < count; i++) {
            TestUtils.createTestBucket(mgr, 'test' + i)
        }
        String id1 = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getCanonicalId()
        assert (mgr.lookupBucketsByOwner(id1).size() == count)

        Bucket[] buckets = (Bucket[]) mgr.lookupBucketsByOwner(id1).toArray(new Bucket[0])
        for (int i = 0; i < buckets.length - 1; i++) {
            println 'Bucket -  ' + buckets[i]
            assert (buckets[i].getBucketName().compareTo(buckets[i + 1].getBucketName()) <= 0)
        }

    }

    @Test
    public void testListByUser() {
        int count = 10
        for (int i = 0; i < count; i++) {
            TestUtils.createTestBucket(mgr, 'test' + i)
        }
        def userId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()

        assert (mgr.lookupBucketsByUser(userId).size() == 10)

        Bucket[] buckets = (Bucket[]) mgr.lookupBucketsByOwner(userId).toArray(new Bucket[0])
        for (int i = 0; i < buckets.length - 1; i++) {
            println 'Bucket -  ' + buckets[i]
            assert (buckets[i].getBucketName().compareTo(buckets[i + 1].getBucketName()) <= 0)
        }
    }

    private Bucket initializeBucket(String name, String accountName) {
        AccessControlPolicy acp = new AccessControlPolicy()
        CanonicalUser owner = new CanonicalUser(Accounts.lookupAccountByName(accountName).getCanonicalId(), "")
        acp.setOwner(owner)
        acp.setAccessControlList(new AccessControlList())
        acp.getAccessControlList().setGrants(new ArrayList<Grant>())
        acp.getAccessControlList().getGrants().add(new Grant(new Grantee(owner), ObjectStorageProperties.Permission.FULL_CONTROL.toString()))

        return mgr.persistBucketInCreatingState(name, acp, UnitTestSupport.getUsersByAccountName(accountName).first(), "")
    }

    @Test
    public void testUpdateBucketSize() {
        def bucketName = 'testbucket1'
        Bucket bucket = TestUtils.createTestBucket(mgr, bucketName)
        assert (mgr.lookupBucket(bucketName).getBucketSize() == 0)

        mgr.updateBucketSize(bucket, 100)
        assert (mgr.lookupBucket(bucketName).getBucketSize() == 100)

        mgr.updateBucketSize(bucket, 100)
        assert (mgr.lookupBucket(bucketName).getBucketSize() == 200)

        mgr.updateBucketSize(bucket, -10)
        assert (mgr.lookupBucket(bucketName).getBucketSize() == 190)

        def b = mgr.transitionBucketToState(mgr.lookupBucket(bucketName), BucketState.deleting)
        mgr.deleteBucketMetadata(b)
    }

    @Test
    public void testCountByUser() {
        Bucket b;
        for (int i = 0; i < 10; i++) {
            b = TestUtils.createTestBucket(mgr, 'bucket' + i)
            assert (b != null && b.getState().equals(BucketState.extant))
        }
        assert (mgr.countBucketsByUser(b.getOwnerIamUserId()) == 10)
    }

    @Test
    public void testCountByAccount() {
        Bucket b;
        String account = UnitTestSupport.getTestAccounts().first()
        String canonicalId = Accounts.lookupAccountByName(account).getCanonicalId();
        for (int i = 0; i < 10; i++) {
            b = TestUtils.createTestBucket(mgr, 'bucket' + i, account)
            println 'Created bucket owned by ' + b.getOwnerCanonicalId()
            assert (b != null && b.getState().equals(BucketState.extant))
        }

        assert (mgr.countBucketsByAccount(canonicalId) == 10)
    }

    @Test
    public void testSetAcpString() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)
        assert (b != null)
        assert (b.getAccessControlPolicy() != null)
        int logDeliveryBitmap = S3AccessControlledEntity.BitmapGrant.add(ObjectStorageProperties.Permission.WRITE, S3AccessControlledEntity.BitmapGrant.translateToBitmap(ObjectStorageProperties.Permission.READ_ACP))
        int fullControlBitmap = S3AccessControlledEntity.BitmapGrant.translateToBitmap(ObjectStorageProperties.Permission.FULL_CONTROL)
        def aclString = '{"' + b.getOwnerCanonicalId() + '":' + fullControlBitmap + ',"http://acs.amazonaws.com/groups/s3/LogDelivery":' + logDeliveryBitmap + '}'
        mgr.setAcp(b, aclString)
        Bucket updatedB = mgr.lookupBucket(bucketName)
        assert (updatedB.getAccessControlPolicy() != null)
        String genAclString = S3AccessControlledEntity.marshallAcpToString(updatedB.getAccessControlPolicy())
        //TODO: use unordered comparison
        //assert(S3AccessControlledEntity.genAclString == aclString)
    }

    @Test
    public void testSetAcp() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)
        assert (b != null)
        assert (b.getAccessControlPolicy() != null)
        assert (b.can(ObjectStorageProperties.Permission.FULL_CONTROL, b.getOwnerCanonicalId()))

        AccessControlPolicy acp = b.getAccessControlPolicy()
        acp.getAccessControlList().getGrants().add(TestUtils.TEST_ACCOUNT2_FULLCONTROL_GRANT)
        mgr.setAcp(b, acp)
        Bucket updatedB = mgr.lookupBucket(bucketName)
        assert (updatedB.getAccessControlPolicy() != null)
        assert (updatedB.getAccessControlPolicy().equals(acp))
        assert (updatedB.can(ObjectStorageProperties.Permission.FULL_CONTROL, TestUtils.TEST_CANONICALUSER_2.getID()))
        assert (updatedB.can(ObjectStorageProperties.Permission.FULL_CONTROL, b.getOwnerCanonicalId()))

    }

    @Test
    public void testConcurrentBucketUpdate() {
        //Tests to ensure interleaved transactions are handled properly.
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)

        Bucket lookup1 = mgr.lookupExtantBucket(bucketName)
        Bucket lookup2 = mgr.lookupExtantBucket(bucketName)
        Bucket postUpdate1 = mgr.setVersioning(lookup1, ObjectStorageProperties.VersioningStatus.Enabled)
        assert (postUpdate1.getVersioning() == ObjectStorageProperties.VersioningStatus.Enabled)

        Bucket postUpdate2 = mgr.updateBucketSize(lookup2, 10)
        assert (postUpdate2.getVersioning() == ObjectStorageProperties.VersioningStatus.Enabled)
        assert (postUpdate2.getBucketSize() == 10)

        Bucket postupdate3 = mgr.setVersioning(postUpdate1, ObjectStorageProperties.VersioningStatus.Suspended)
        assert (postupdate3.getBucketSize() == 10)
        assert (postupdate3.getVersioning() == ObjectStorageProperties.VersioningStatus.Suspended)
    }

    @Ignore
    @Test
    public void testSetLoggingStatus() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetVersioning() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)
        b = mgr.lookupBucket(bucketName)
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Disabled))

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Enabled)
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Enabled))

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Suspended)
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Suspended))

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Enabled)
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Enabled))

        try {
            b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Disabled)
            fail('Should not allow Enabled->Disabled versioning transition')
        } catch (Exception e) {
            println 'Correctly caught exception on bad state transition enabled->disabled'
            assert (mgr.lookupBucket(b.getBucketName()).getVersioning().equals(ObjectStorageProperties.VersioningStatus.Enabled))
        }

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Suspended)
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Suspended))

        try {
            b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Disabled)
            fail('Should not allow Suspended->Disabled versioning transition')
        } catch (Exception e) {
            println 'Correctly caught exception on bad state transition suspended->disabled'
            assert (mgr.lookupBucket(b.getBucketName()).getVersioning().equals(ObjectStorageProperties.VersioningStatus.Suspended))
        }
    }

    @Test
    public void testGenerateObjectVersionId() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)
        b = mgr.lookupBucket(bucketName)
        String versionId = b.generateObjectVersionId()
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Disabled) && versionId == 'null')

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Enabled)
        versionId = b.generateObjectVersionId()
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Enabled) && versionId != 'null')

        b = mgr.setVersioning(b, ObjectStorageProperties.VersioningStatus.Suspended)
        versionId = b.generateObjectVersionId()
        assert (b.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Suspended) && versionId == 'null')
    }

    @Test
    public void testTotalSizeOfAllBuckets() {
        Bucket b;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            b = TestUtils.createTestBucket(mgr, 'bucket' + i)
            assert (b != null && b.getState().equals(BucketState.extant))
            mgr.updateBucketSize(b, 10)
            assert (mgr.lookupBucket(b.getBucketName()).getBucketSize() == 10)
            sum += 10
        }

        assert (mgr.totalSizeOfAllBuckets() == sum)
    }

    @Test
    public void testLookupBucketsByOwner() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)

        assert (mgr.lookupExtantBucket(bucketName) != null)

        def buckets = mgr.lookupBucketsByOwner(b.getOwnerCanonicalId())
        assert (buckets != null)
        assert (buckets.size() == 1)
        assert (buckets.first().getBucketName() == bucketName)

        mgr.transitionBucketToState(b, BucketState.deleting)
        buckets = mgr.lookupBucketsByOwner(b.getOwnerCanonicalId())
        assert (buckets != null)
        assert (buckets.size() == 0)


        def accountName2 = UnitTestSupport.getTestAccounts().getAt(1) //2nd
        def bucketName2 = 'testbucket2'
        Bucket b2 = TestUtils.initializeBucket(mgr, bucketName2, accountName2)

        buckets = mgr.lookupBucketsByOwner(b.getOwnerCanonicalId())
        assert (buckets != null)
        assert (buckets.size() == 0)

        buckets = mgr.lookupBucketsByOwner(b2.getOwnerCanonicalId())
        assert (buckets != null)
        assert (buckets.size() == 0)

    }

    @Test
    public void testLookupBucketsByUserId() {
        def bucketName = 'testbucket1'
        Bucket b = TestUtils.createTestBucket(mgr, bucketName)

        assert (mgr.lookupExtantBucket(bucketName) != null)

        def buckets = mgr.lookupBucketsByUser(b.getOwnerIamUserId())
        assert (buckets != null)
        assert (buckets.size() == 1)
        assert (buckets.first().getBucketName() == bucketName)

        mgr.transitionBucketToState(b, BucketState.deleting)
        buckets = mgr.lookupBucketsByOwner(b.getOwnerCanonicalId())
        assert (buckets != null)
        assert (buckets.size() == 0)

        def accountName2 = UnitTestSupport.getTestAccounts().getAt(1) //2nd
        def bucketName2 = 'testbucket2'
        Bucket b2 = TestUtils.initializeBucket(mgr, bucketName2, accountName2)

        buckets = mgr.lookupBucketsByUser(b.getOwnerIamUserId())
        assert (buckets != null)
        assert (buckets.size() == 0)

        buckets = mgr.lookupBucketsByUser(b2.getOwnerIamUserId())
        assert (buckets != null)
        assert (buckets.size() == 0)

    }

}
