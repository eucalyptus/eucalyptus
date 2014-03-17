package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.Transactions
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException
import com.eucalyptus.objectstorage.metadata.BucketMetadataManager
import com.eucalyptus.objectstorage.metadata.ObjectMetadataManager
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass;

import com.eucalyptus.objectstorage.entities.Bucket;
import org.apache.log4j.Logger;
import static org.junit.Assert.fail;
import org.junit.Test;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.entities.ObjectEntity

import javax.persistence.EntityTransaction;

/**
 * Tests the ObjectMetadataManager implementations.
 * @author zhill
 *
 */
public class ObjectMetadataManagerTest {
    private static final Logger LOG = Logger.getLogger(ObjectMetadataManagerTest.class);

    static BucketMetadataManager mgr = BucketMetadataManagers.getInstance()
    static ObjectMetadataManager objMgr = ObjectMetadataManagers.getInstance()

    @Before
    public void setUp() throws Exception {
        mgr.start()
        objMgr.start()
        UnitTestSupport.flushObjects()
        UnitTestSupport.flushBuckets()
        TestUtils.initTestAccountsAndAcls()
    }

    @After
    public void tearDown() throws Exception {
        mgr.stop()
        objMgr.stop()
        UnitTestSupport.flushObjects()
        UnitTestSupport.flushBuckets()
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        UnitTestSupport.setupOsgPersistenceContext()
        UnitTestSupport.setupAuthPersistenceContext()
        UnitTestSupport.initializeAuth(2,2)


    }

    @AfterClass
    public static void teardownAfterClass() throws Exception {
        UnitTestSupport.tearDownOsgPersistenceContext()
        UnitTestSupport.tearDownAuthPersistenceContext()
    }

	@Test
	public void testObjectListing() {
		LOG.info("Testing object listing");
		
		int entityCount = 10;
		String key = "objectkey";

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        def objs = TestUtils.createNObjects(objMgr, entityCount, bucket, key, 100, usr)
        assert(objs != null && objs.size() == entityCount)

		try {
			PaginatedResult<ObjectEntity> r = objMgr.listPaginated(bucket, 100, null, null, null);

			for(ObjectEntity e : r.getEntityList()) {
				println e.toString()
			}
			
			assert(r.getEntityList().size() == entityCount);
				
		} catch(Exception e) {
			LOG.error("Transaction error", e);
			fail("Failed getting listing");
			
		} finally {
			for(ObjectEntity obj : objs) {
				try {
					objMgr.delete(obj);
				} catch(Exception e) {
					LOG.error("Error deleteing entity: " + obj.toString(), e);
				}
			}
		}
	}
	
	/*
	 * Tests create, lookup, delete lifecycle a single object
	 */
	@Test
	public void testCreateDelete() {
		String key = "testkey";
		long contentLength = 100;

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert((bucket = mgr.lookupBucket(bucket.getName())) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
        ObjectEntity object1 = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)

        object1 = objMgr.initiateCreation(object1);
        assert(objMgr.lookupObjectsInState(bucket, key, null, ObjectState.creating).first().equals(object1))

        def object1Extant = objMgr.finalizeCreation(object1, new Date(), "fakeetag")

        assert(object1Extant.getState().equals(ObjectState.extant))
        assert(object1Extant.getObjectModifiedTimestamp() != null)
        assert(objMgr.lookupObject(bucket, key, null).equals(object1Extant))

        def obj1Deleting = objMgr.transitionObjectToState(object1Extant, ObjectState.deleting);
        assert(obj1Deleting.getState().equals(ObjectState.deleting))

        try {
            objMgr.lookupObject(object1.getBucket(), object1.getObjectKey(), null)
            fail("Should get not found exception on lookup of deleted object")
        } catch(NoSuchElementException e) {
            LOG.info("Correctly got not-found exception on lookup of non-existent object")
        }

        objMgr.delete(obj1Deleting)
        assert(objMgr.lookupObjectsInState(obj1Deleting.getBucket(), obj1Deleting.getObjectKey(), null, ObjectState.deleting).size() == 0)

        try {
            objMgr.lookupObject(object1.getBucket(), object1.getObjectKey(), null)
            fail("Should get not found exception on lookup of deleted object")
        } catch(NoSuchElementException e) {
            LOG.info("Correctly got not-found exception on lookup of non-existent object")
        }

        //Verify on db directly
        try {
            ObjectEntity search = new ObjectEntity().withBucket(object1.getBucket()).withKey(object1.getObjectKey())
            ObjectEntity result = Transactions.find(search)
            fail("Should get not found exception on lookup of deleted object: " + result.getObjectUuid() + " - " + result.getState().toString())
        } catch(NoSuchElementException e) {
            LOG.info("Correctly got not-found exception on lookup of non-existent object")
        }

    }

    @Test
    public void testCountObjectsByBucket() {
        String key = "testkey";
        long contentLength = 100;

        Bucket bucket1 = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket1 != null)
        assert(mgr.lookupBucket(bucket1.getName()) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        def objList = TestUtils.createNObjects(objMgr, 10, bucket1, key, contentLength, usr)
        assert(objList.size() == 10)

        Bucket bucket2 = TestUtils.createTestBucket(mgr, "testbucket2")
        assert(bucket2 != null)
        assert(mgr.lookupBucket(bucket2.getName()) != null)

        for (int i = 0; i < 10 ; i++) {
            ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket2, key + i, contentLength, usr)
            entity = objMgr.initiateCreation(entity)
            assert(entity.getBucket().equals(bucket2))
            assert(objMgr.lookupObjectsInState(bucket2, key + i, entity.getVersionId(), ObjectState.creating).first().equals(entity))
            assert(objMgr.countValid(bucket1) == 10)
            assert(objMgr.countValid(bucket2) == i)

            entity = objMgr.finalizeCreation(entity, new Date(), UUID.randomUUID().toString())
            assert(objMgr.lookupObjectsInState(bucket2, entity.getObjectKey(), entity.getVersionId(), ObjectState.extant).first().equals(entity))
            assert(objMgr.countValid(bucket1) == 10)
            assert(objMgr.countValid(bucket2) == i + 1)
        }

    }

    @Test
    public void testListPaginatedSinglePage() {
        def keyPrefixes = ['aa', 'bb', 'cc', 'dd', 'ee']
        def bucketName = 'bucket'
        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
        Bucket bucket = TestUtils.createTestBucket(mgr, bucketName)

        PaginatedResult<ObjectEntity> list1 = objMgr.listPaginated(bucket, 1000, null, null, null)
        assert(list1 != null)
        assert(list1.getEntityList().size() == 0)
        assert(list1.getCommonPrefixes().size() == 0)
        assert(!list1.getIsTruncated())
        assert(list1.getLastEntry() == null)

        keyPrefixes.each { k ->
            TestUtils.createNObjects(objMgr, 10, bucket, (String)k, 100, usr)
        }

        PaginatedResult<ObjectEntity> listing = objMgr.listPaginated(bucket, 1000, null, null, null)

        assert(listing != null)
        assert(listing.getEntityList().size() == keyPrefixes.size() * 10)
        assert(!listing.getIsTruncated())

        assert(verifyListingOrder(listing))
    }

    private static boolean verifyListingOrder(PaginatedResult<ObjectEntity> listing) {
        ObjectEntity last = null;
        for(ObjectEntity e : listing.getEntityList()) {
            if(last != null) {
                assert(e.getState().equals(ObjectState.extant))
                assert((e.getIsDeleteMarker() && e.getSize() == -1) ||
                        (!e.getIsDeleteMarker() && e.getSize() > 0))
                if(e.getObjectKey().compareTo(last.getObjectKey()) < 0) {
                    return false;
                }
                if(e.getObjectKey() == last.getObjectKey()) {
                    //version check, ensure latest->oldest order
                    if(e.getObjectModifiedTimestamp().compareTo(last.getObjectModifiedTimestamp()) >= 0) {
                        return false;
                    }
                }
            }
            last = e;
        }
        return true;
    }

    public void testListPaginatedMultiPage() {

    }

    public void testListPaginatedPrefix() {

    }

    public void testListPaginatedDelim() {

    }

    public void testListPaginatedPagedPrefixDelim() {

    }

    public void testListVersionsPaginated() {

    }

    @Test
    public void testTransitionObjectToState() {
        String bucketName = 'bucket'
        String key = 'key'
        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
        Bucket bucket = TestUtils.createTestBucket(mgr, bucketName)

        //null->creating OK
        def obj1 = ObjectEntity.newInitializedForCreate(bucket, key + "null->creating", 10, usr)
        obj1 = objMgr.transitionObjectToState(obj1, ObjectState.creating)
        assert(obj1.getState().equals(ObjectState.creating))

        //null->Extant FAIL
        def obj2 = ObjectEntity.newInitializedForCreate(bucket, key + "null->extant", 10, usr)
        try {
            obj2 = objMgr.transitionObjectToState(obj2, ObjectState.extant)
            fail('Should have gotten exception that object not in creating state')
        } catch(NoSuchEntityException e) {
            LOG.info('Correctly caught not found exception')
        }

        //null->deleting FAIL
        def obj3 = ObjectEntity.newInitializedForCreate(bucket, key + "null->deleting", 10, usr)
        try {
            obj3 = objMgr.transitionObjectToState(obj3, ObjectState.deleting)
            fail('Should get not-found exception')
        } catch(Exception e) {
            LOG.info("Got correct exception on null->extant state transition of object", e)
        }

        //creating->creating fail
        def obj4 = ObjectEntity.newInitializedForCreate(bucket, key + "creating->creating", 10, usr)
        obj4 = objMgr.transitionObjectToState(obj4, ObjectState.creating)
        try {
            obj4 = objMgr.transitionObjectToState(obj4, ObjectState.creating)
            fail('Object already persisted, cannot re-initialize')
        } catch(MetadataOperationFailureException e) {
            LOG.info('Correct failure on create->create update')
        }

        //creating->extant OK
        def obj5 = ObjectEntity.newInitializedForCreate(bucket, key + "creating->extant", 10, usr)
        obj5 = objMgr.transitionObjectToState(obj5, ObjectState.creating)
        assert(obj5.getState().equals(ObjectState.creating))

        //creating->deleting OK
        def obj6 = ObjectEntity.newInitializedForCreate(bucket, key + "creating->deleting", 10, usr)
        obj6 = objMgr.transitionObjectToState(obj6, ObjectState.creating)
        assert(obj6.getState().equals(ObjectState.creating))

        obj6 = objMgr.transitionObjectToState(obj6, ObjectState.deleting)
        assert(obj6.getState().equals(ObjectState.deleting))

        //extant->deleting OK
        def obj7 = ObjectEntity.newInitializedForCreate(bucket, key + "extant->delete", 10, usr)
        obj7 = objMgr.transitionObjectToState(obj7, ObjectState.creating)
        assert(obj7.getState().equals(ObjectState.creating))

        obj7 = objMgr.transitionObjectToState(obj7, ObjectState.extant)
        assert(obj7.getState().equals(ObjectState.extant))

        obj7 = objMgr.transitionObjectToState(obj7, ObjectState.deleting)
        assert(obj7.getState().equals(ObjectState.deleting))

        //deleting->extant FAIL
        def obj9 = ObjectEntity.newInitializedForCreate(bucket, key + "deleting-extant", 10, usr)
        obj9 = objMgr.transitionObjectToState(obj9, ObjectState.creating)
        assert(obj9.getState().equals(ObjectState.creating))

        obj9 = objMgr.transitionObjectToState(obj9, ObjectState.deleting)
        assert(obj9.getState().equals(ObjectState.deleting))

        try {
            obj9 = objMgr.transitionObjectToState(obj9, ObjectState.extant)
            fail('Should have gotten exception on deleting->extant transitiong')
        } catch(IllegalResourceStateException e) {
            LOG.info('Got correct exception on deleting->extant transition')
        }

        //deleting->creating FAIL
        def obj10 = ObjectEntity.newInitializedForCreate(bucket, key + "deleting->creating", 10, usr)
        obj10 = objMgr.transitionObjectToState(obj10, ObjectState.creating)
        assert(obj10.getState().equals(ObjectState.creating))

        obj10 = objMgr.transitionObjectToState(obj10, ObjectState.deleting)
        assert(obj10.getState().equals(ObjectState.deleting))

        try {
            obj10 = objMgr.transitionObjectToState(obj9, ObjectState.creating)
            fail('Should have gotten exception on deleting->creating transitiong')
        } catch(IllegalResourceStateException e) {
            LOG.info('Got correct exception on deleting->extant transition')
        }
    }

    @Test
    public void testGetFailed() {
        String key = "testkey"
        long contentLength = 100

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)

        Bucket bucket2 = TestUtils.createTestBucket(mgr, "testbucket2")
        assert(bucket2 != null)
        assert(mgr.lookupBucket(bucket2.getName()) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        //Create some other objects to ensure operations don't affect unintended objects
        def immutableBucket2Objs = TestUtils.createNObjects(objMgr, 10, bucket2, "immutableobject", 100, usr)
        def immutableBucket1Objs = TestUtils.createNObjects(objMgr, 10, bucket, "immutableobject", 100, usr)

        assert(objMgr.countValid(bucket) == 10)
        assert(objMgr.countValid(bucket2) == 10)

        ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)
        entity = objMgr.initiateCreation(entity)

        assert(objMgr.countValid(bucket) == 10)
        assert(objMgr.countValid(bucket2) == 10)

        long initialTime = new Long(entity.getCreationExpiration());
        assert(initialTime > 0)

        //mark expiration in the past to force 'failure' case
        entity.setCreationExpiration(System.currentTimeMillis() - 10000)
        try {
            EntityTransaction db = Entities.get(ObjectEntity.class)
            entity = Entities.mergeDirect(entity)
            db.commit()
        } catch(Exception e) {
            fail(e.getMessage())
        }

        def failedObjects = objMgr.lookupFailedObjects()
        assert(entity.getCreationExpiration() < initialTime)
        assert(failedObjects != null && failedObjects.size() == 1)

        assert(objMgr.countValid(bucket) == 10)
        assert(objMgr.countValid(bucket2) == 10)
    }

    @Test
    public void testDoFullRepair() {
        String key = "testkey"
        long contentLength = 100

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)

        Bucket bucket2 = TestUtils.createTestBucket(mgr, "testbucket2")
        assert(bucket2 != null)
        assert(mgr.lookupBucket(bucket2.getName()) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        def immutableObjsBucket1 = TestUtils.createNObjects(objMgr, 10, bucket, "immutablekey", 100, usr)
        def immutableObjsBucket2 = TestUtils.createNObjects(objMgr, 10, bucket2, "immutablekey", 100, usr)
        assert(objMgr.countValid(bucket) == 10)
        assert(objMgr.countValid(bucket2) == 10)


        def parallelCount = 10
        for (int i = 0; i < parallelCount ; i++) {
            ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)
            objMgr.finalizeCreation(objMgr.initiateCreation(entity), new Date(), UUID.randomUUID().toString())
        }

        assert(objMgr.countValid(bucket) == 11)
        assert(objMgr.countValid(bucket2) == 10)

        List<ObjectEntity> objectEntities

        objectEntities = objMgr.lookupObjectsInState(bucket, key, 'null', ObjectState.extant)
        assert(objectEntities.size() == 1)

        Collections.sort(objectEntities, new Comparator<ObjectEntity>() {
            @Override
            int compare(ObjectEntity objectEntity, ObjectEntity objectEntity2) {
                //Sort reverse, desc order
                return objectEntity2.getObjectModifiedTimestamp().compareTo(objectEntity.getObjectModifiedTimestamp())
            }
        })

        objectEntities.each { println it.getObjectUuid() + ' - ' + it.getObjectModifiedTimestamp()}

        assert(objectEntities.count { it.getIsLatest()  } == 1 )

        def mostRecentObj = objMgr.lookupObject(bucket, key, null)
        //Ensure the get call got the most recent
        assert(mostRecentObj.getObjectUuid().equals(objectEntities.first().getObjectUuid()))

        //Do repair
        objMgr.cleanupInvalidObjects(bucket, key)
        objectEntities = objMgr.lookupObjectsInState(bucket, key, 'null', ObjectState.extant)
        def mostRecentObj2 = objMgr.lookupObject(bucket, key, null)
        assert(mostRecentObj2.getObjectUuid().equals(mostRecentObj.getObjectUuid()))

        //Ensure only one has 'isLatest' set
        assert(objectEntities.count { it.getIsLatest() } == 1)
        assert(objectEntities.find { it.getIsLatest() }.getObjectUuid().equals(mostRecentObj.getObjectUuid()))

        assert(objMgr.countValid(bucket) == 10 + 1) //all parallel should be reduced to 1 record
        assert(objMgr.countValid(bucket2) == 10)
    }

    @Test
    public void testConcurrentPut() {
        String key = "testkey"
        long contentLength = 100

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)

        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        def parallelCount = 10
        List<ObjectEntity> objs = Lists.newArrayList();
        for (int i = 0; i < parallelCount ; i++) {
            ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)
            objs.add(objMgr.initiateCreation(entity))
        }

        objMgr.lookupObjectsInState(bucket, key, null, ObjectState.creating).each { println it.getObjectUuid() + ' - ' + it.getObjectModifiedTimestamp() }

        //all in 'creating' at once.
        assert(objMgr.lookupObjectsInState(bucket, key, null, ObjectState.creating).size() == parallelCount)

        //finalize all.
        objs.each { object ->
                objMgr.finalizeCreation((ObjectEntity)object, new Date(), UUID.randomUUID().toString())
        }

        assert(objMgr.lookupObjectsInState(bucket, key, null, ObjectState.creating).size() == 0)
        assert(objMgr.lookupObjectsInState(bucket, key, null, ObjectState.extant).size() == 1) //All reduces to a single last-write-wins object

        objMgr.lookupObjectsInState(bucket, key, null, ObjectState.extant).each { println it.getObjectUuid() + ' - ' + it.getObjectModifiedTimestamp() }
    }


    @Test
    public void testCountValid() {
        String key = "testkey"
        long contentLength = 100

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)
        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        def objList = TestUtils.createNObjects(objMgr, 10, bucket, key, contentLength, usr)
        assert(objList.size() == 10)

        assert(objMgr.countValid(bucket) == 10)

        ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)
        entity = objMgr.initiateCreation(entity);

        assert(objMgr.countValid(bucket) == 10)

        objMgr.finalizeCreation(entity, new Date(), UUID.randomUUID().toString())
        assert(objMgr.countValid(bucket) == 11)

        objMgr.transitionObjectToState(entity, ObjectState.deleting);
        assert(objMgr.countValid(bucket) == 10)
    }

    @Test
    public void testUpdateCreationTimeout() {
        String key = "testkey"
        long contentLength = 100

        Bucket bucket = TestUtils.createTestBucket(mgr, "testbucket")
        assert(bucket != null)
        assert(mgr.lookupBucket(bucket.getName()) != null)
        User usr = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())

        ObjectEntity entity = ObjectEntity.newInitializedForCreate(bucket, key, contentLength, usr)
        entity = objMgr.initiateCreation(entity)

        long initialTime = entity.getCreationExpiration()
        assert(initialTime > 0)

        entity = objMgr.updateCreationTimeout(entity)
        assert(entity.getCreationExpiration() > initialTime)

        entity = objMgr.finalizeCreation(entity, new Date(), UUID.randomUUID().toString())
        assert(entity.getCreationExpiration() == null)
    }
}
