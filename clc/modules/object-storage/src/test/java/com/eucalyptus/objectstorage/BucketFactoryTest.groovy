package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.User
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType
import com.eucalyptus.objectstorage.providers.InMemoryProvider
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient
import com.eucalyptus.objectstorage.providers.s3.S3ProviderClient
import com.eucalyptus.objectstorage.providers.s3.S3ProviderConfiguration
import com.eucalyptus.objectstorage.providers.walrus.WalrusProviderClient
import com.eucalyptus.objectstorage.util.AclUtils
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.AccessControlList
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.CanonicalUser
import com.eucalyptus.storage.msgs.s3.Grant
import com.eucalyptus.storage.msgs.s3.Grantee
import com.google.common.base.Strings
import groovy.transform.CompileStatic
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests the BucketFactory as an integration test
 *
 */
@CompileStatic
public class BucketFactoryTest {
    static ObjectStorageProviderClient provider = null
    static String configValue = null
    static CanonicalUser TEST_CANONICALUSER_1
    static CanonicalUser TEST_CANONICALUSER_2
    static AccessControlList TEST_ACCOUNT1_PRIVATE_ACL = new AccessControlList()
    static AccessControlList TEST_ACCOUNT2_PRIVATE_ACL = new AccessControlList()
    static Grant TEST_ACCOUNT1_FULLCONTROL_GRANT
    static Grant TEST_ACCOUNT2_FULLCONTROL_GRANT

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        UnitTestSupport.setupOsgPersistenceContext()
        UnitTestSupport.setupAuthPersistenceContext()
        UnitTestSupport.initializeAuth(2, 2)

        initUsers()
        initProvider()
    }

    static void initUsers() {
        Account accnt = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first())
        TEST_CANONICALUSER_1 = new CanonicalUser(accnt.getCanonicalId(), accnt.getUsers().get(0).getInfo("email"))

        accnt = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts()[1])
        TEST_CANONICALUSER_2 = new CanonicalUser(accnt.getCanonicalId(), accnt.getUsers().get(0).getInfo("email"))

        TEST_ACCOUNT1_FULLCONTROL_GRANT = new Grant(new Grantee(TEST_CANONICALUSER_1), ObjectStorageProperties.Permission.FULL_CONTROL.toString());
        TEST_ACCOUNT1_PRIVATE_ACL.setGrants(new ArrayList<Grant>());
        TEST_ACCOUNT1_PRIVATE_ACL.getGrants().add(TEST_ACCOUNT1_FULLCONTROL_GRANT);


        TEST_ACCOUNT2_FULLCONTROL_GRANT = new Grant(new Grantee(TEST_CANONICALUSER_2), ObjectStorageProperties.Permission.FULL_CONTROL.toString());
        TEST_ACCOUNT2_PRIVATE_ACL.setGrants(new ArrayList<Grant>());
        TEST_ACCOUNT2_PRIVATE_ACL.getGrants().add(TEST_ACCOUNT2_FULLCONTROL_GRANT);

        //Ensure there are accesskeys for each user
        UnitTestSupport.getTestAccounts().each { account ->
            UnitTestSupport.getUsersByAccountName((String) account).each { userId ->
                Accounts.lookupUserById((String) userId).createKey()
            }
        }
    }

    static void initProvider() {
        configValue = System.getProperty("provider", "mem")
        println 'Using provider ' + configValue

        S3ProviderConfiguration.S3AccessKey = System.getProperty("accessKey")
        S3ProviderConfiguration.S3SecretKey = System.getProperty("secretKey")
        S3ProviderConfiguration.S3Endpoint = System.getProperty("endpoint")

        switch (configValue) {
            case 's3':
                assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.S3Endpoint))
                assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.S3AccessKey))
                assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.S3SecretKey))
                println 'Using endpoint ' + S3ProviderConfiguration.S3Endpoint
                provider = new S3ProviderClient()
                break
            case 'walrus':
                provider = new WalrusProviderClient()
                break
            case 'mem':
                provider = new InMemoryProvider()
                break
            default:
                throw new RuntimeException('Unknown provider specified: ' + configValue)
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        UnitTestSupport.tearDownOsgPersistenceContext()
        UnitTestSupport.tearDownAuthPersistenceContext()
    }

    @Before
    public void setUp() throws Exception {
        provider.start();
        BucketMetadataManagers.getInstance().start();
        ObjectMetadataManagers.getInstance().start();
        UnitTestSupport.flushBuckets()
        UnitTestSupport.flushObjects()
    }

    @After
    public void tearDown() throws Exception {
        UnitTestSupport.flushBuckets()
        UnitTestSupport.flushObjects()
        BucketMetadataManagers.getInstance().stop();
        ObjectMetadataManagers.getInstance().stop();
        provider.stop();
    }

    @Test
    public void testCreateBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TEST_CANONICALUSER_1)
        acp.setAccessControlList(TEST_ACCOUNT1_PRIVATE_ACL)
        String iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()

        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)

        Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                initializedBucket,
                correlationId,
                Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()));

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }
        assert (response.getBucketList().getBuckets().size() == 1)
        assert (response.getBucketList().getBuckets().get(0).getName().equals(createdBucket.getBucketUuid()))

        //Verify in our metadata
        Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
        assert (bucket != null)
        assert (bucket.getState().equals(BucketState.extant))
        assert (bucket.equals(createdBucket))
    }

    @Test
    public void testCreateDuplicateBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TEST_CANONICALUSER_1)
        acp.setAccessControlList(TEST_ACCOUNT1_PRIVATE_ACL)
        String iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()

        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)

        Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                initializedBucket,
                correlationId,
                Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()));

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }
        assert (response.getBucketList().getBuckets().size() == 1)
        assert (response.getBucketList().getBuckets().get(0).getName().equals(createdBucket.getBucketUuid()))

        //Verify in our metadata
        Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
        assert (bucket != null)
        assert (bucket.getState().equals(BucketState.extant))
        assert (bucket.equals(createdBucket))

        try {
            Bucket initializedDuplicateBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)
            Bucket createdDuplicateBucket = OsgBucketFactory.getFactory().createBucket(provider,
                    initializedDuplicateBucket,
                    correlationId,
                    Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()));
            fail('Should have had exception')
        } catch (BucketAlreadyExistsException e) {
            //Correct exception
        }

        //set bucket to deleting
        BucketMetadataManagers.getInstance().transitionBucketToState(bucket, BucketState.deleting);

        //Should work
        try {
            Bucket initializedDuplicateBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)
            Bucket createdDuplicateBucket = OsgBucketFactory.getFactory().createBucket(provider,
                    initializedDuplicateBucket,
                    correlationId,
                    Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()));
            //Should be okay
            Bucket newBucket = BucketMetadataManagers.getInstance().lookupExtantBucket(bucket.getBucketName())
            assert (newBucket.getBucketUuid().equals(createdDuplicateBucket.getBucketUuid()))
            assert (newBucket.getState().equals(BucketState.extant))
        } catch (BucketAlreadyExistsException e) {
            fail('Should not have exception. name should be clear')
        }
    }

    @Test
    public void testCreateNoAclBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"

        User user = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
        String canonicalId = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().first()).getCanonicalId()
        def tmpAcp = new AccessControlPolicy()
        tmpAcp.setAccessControlList(new AccessControlList())
        AccessControlPolicy acp = AclUtils.processNewResourcePolicy(user, tmpAcp, canonicalId)

        String iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()
        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)

        Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                initializedBucket,
                correlationId,
                user);

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }
        assert (response.getBucketList().getBuckets().size() == 1)
        assert (response.getBucketList().getBuckets().get(0).getName().equals(createdBucket.getBucketUuid()))

        //Verify in our metadata
        Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
        assert (bucket != null)
        assert (bucket.getState().equals(BucketState.extant))
        assert (bucket.equals(createdBucket))
        assert (bucket.getAccessControlPolicy() != null)
    }

    @Test
    public void testFailCreateBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TEST_CANONICALUSER_1)
        acp.setAccessControlList(TEST_ACCOUNT1_PRIVATE_ACL)
        def iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()
        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)

        ((InMemoryProvider) provider).failBucketPut = InMemoryProvider.FAIL_TYPE.INTERNAL_ERROR
        try {

            Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                    initializedBucket,
                    correlationId,
                    null)
            fail('Should have thrown exception for backend failure')
        } catch (InternalErrorException e) {
            println 'Correctly caught exception for failed creation ' + e.getMessage()
        }

        ((InMemoryProvider) provider).failBucketPut = InMemoryProvider.FAIL_TYPE.NONE

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }
        assert (response.getBucketList().getBuckets().size() == 0)

        //Verify in our metadata
        try {
            Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
            assert ('Should have exceptioned out on lookup for failed bucket')
        } catch (Exception e) {
            println 'Correctly failed to find bucket that failed on creation'
        }
    }

    @Test
    public void testDeleteBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TEST_CANONICALUSER_1)
        acp.setAccessControlList(TEST_ACCOUNT1_PRIVATE_ACL)
        String iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()

        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)
        Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                initializedBucket,
                correlationId,
                null)

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }

        assert (response.getBucketList().getBuckets().size() == 1)
        assert (response.getBucketList().getBuckets().get(0).getName().equals(createdBucket.getBucketUuid()))

        //Verify in our metadata
        Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
        assert (bucket != null)
        assert (bucket.getState().equals(BucketState.extant))
        assert (bucket.equals(createdBucket))

        //Delete it
        OsgBucketFactory.getFactory().deleteBucket(provider,
                bucket,
                correlationId,
                null)

        //Verify
        response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }

        assert (response.getBucketList().getBuckets().size() == 0)
    }

    @Test
    public void testFailDeleteBucket() {
        String bucketName = "unittestbucket"
        String location = ""
        String correlationId = "123456789"
        AccessControlPolicy acp = new AccessControlPolicy()
        acp.setOwner(TEST_CANONICALUSER_1)
        acp.setAccessControlList(TEST_ACCOUNT1_PRIVATE_ACL)
        String iamUserId = UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first()

        Bucket initializedBucket = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location)
        Bucket createdBucket = OsgBucketFactory.getFactory().createBucket(provider,
                initializedBucket,
                correlationId,
                null)

        //Verify on the backend directly
        ListAllMyBucketsType listRequest = new ListAllMyBucketsType();
        ListAllMyBucketsResponseType response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }

        assert (response.getBucketList().getBuckets().size() == 1)
        assert (response.getBucketList().getBuckets().get(0).getName().equals(createdBucket.getBucketUuid()))

        //Verify in our metadata
        Bucket bucket = BucketMetadataManagers.getInstance().lookupBucket(bucketName)
        assert (bucket != null)
        assert (bucket.getState().equals(BucketState.extant))
        assert (bucket.equals(createdBucket))

        ((InMemoryProvider) provider).failBucketDelete = InMemoryProvider.FAIL_TYPE.INTERNAL_ERROR

        //Delete it...fails. Should result in 'deleting' state
        OsgBucketFactory.getFactory().deleteBucket(provider,
                bucket,
                correlationId,
                null)

        ((InMemoryProvider) provider).failBucketDelete = InMemoryProvider.FAIL_TYPE.NONE

        try {
            Bucket b = BucketMetadataManagers.getInstance().lookupBucketByUuid(bucket.getBucketUuid())
            assert (b != null)
            assert (b.getState().equals(BucketState.deleting))
        } catch (Exception e) {
            fail('Got exception on lookup for deleting bucket: ' + e.getMessage())
        }

        def deletingBuckets = BucketMetadataManagers.getInstance().getBucketsForDeletion()
        assert (deletingBuckets != null)
        assert (deletingBuckets.size() == 1)
        assert (deletingBuckets.first().getState().equals(BucketState.deleting))
        //Ensure the bucketname is different since it is in deleting state
        assert (deletingBuckets.first().getBucketName() == null)

        //Verify
        response = provider.listAllMyBuckets(listRequest)
        assert (response.getBucketList() != null)
        response.getBucketList().each { b ->
            println 'Found bucket ' + b.toString()
        }
        assert (response.getBucketList().getBuckets().size() == 1)

        //Now really delete it. Ensure retry works

        //Delete it...fails. Should result in 'deleting' state
        OsgBucketFactory.getFactory().deleteBucket(provider,
                bucket,
                correlationId,
                null)

    }
}
