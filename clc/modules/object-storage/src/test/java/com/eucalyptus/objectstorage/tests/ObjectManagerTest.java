package com.eucalyptus.objectstorage.tests;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.CallableWithRollback;
import com.eucalyptus.objectstorage.ObjectManager;
import com.eucalyptus.objectstorage.ObjectManagers;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.util.OSGUtil;

/**
 * Tests the ObjectManager implementations.
 * @author zhill
 *
 */
//Manual testing only, for now
public class ObjectManagerTest {
	private static final Logger LOG = Logger.getLogger(ObjectManagerTest.class);
	static ObjectManager objectManager = ObjectManagers.getInstance();
	static User fakeUser;
	
	protected static String generateVersion() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	public static ObjectEntity generateFakePendingEntity(String bucket, String key, boolean useVersioning) throws Exception {
		String versionId = useVersioning ? generateVersion() : null;
		ObjectEntity obj = new ObjectEntity(bucket, key, versionId);
		obj.initializeForCreate(bucket, key, versionId, UUID.randomUUID().toString(), 100, fakeUser);
		return obj;
	}
	
	public static ObjectEntity generateFakeValidEntity(String bucket, String key, boolean useVersioning) throws Exception {
		String versionId = useVersioning ? generateVersion() : null;
		ObjectEntity obj = new ObjectEntity(bucket, key, versionId);		
		obj.initializeForCreate(bucket, key, versionId, UUID.randomUUID().toString(), 100, fakeUser);
		obj.setObjectModifiedTimestamp(new Date());
		return obj;
	}
	
	public static ObjectEntity generateFakeDeletingEntity(String bucket, String key, boolean useVersioning) throws Exception {
		ObjectEntity obj = generateFakeValidEntity(bucket, key, useVersioning);		
		obj.setDeletedTimestamp(new Date());
		return obj;
	}
	
	public static PutObjectResponseType getFakeSuccessfulPUTResponse(boolean generateVersionId) {
		PutObjectResponseType resp = new PutObjectResponseType();
		resp.setLastModified(OSGUtil.dateToHeaderFormattedString(new Date()));				

		if(generateVersionId) {
			resp.setVersionId(UUID.randomUUID().toString().replace("-", ""));
		} else {
			resp.setVersionId(null); //no versioning
		}
		resp.setEtag(UUID.randomUUID().toString().replace("-", ""));
		resp.setSize(100L);
		return resp;
	}
	
	@Test
	public void testObjectListing() {
		LOG.info("Testing object listing");
		
		int entityCount = 10;
		ObjectEntity testEntity = null;
		String key = "objectkey";
		String bucketName = "testbucket_" + UUID.randomUUID().toString().replace("-", "");
		ArrayList<ObjectEntity> testEntities = new ArrayList<ObjectEntity>(entityCount);
		final boolean useVersioning = false;
		
		CallableWithRollback<PutObjectResponseType, Boolean> fakeModifier = new CallableWithRollback<PutObjectResponseType, Boolean>() {

			@Override
			public PutObjectResponseType call() throws S3Exception, Exception {
				return getFakeSuccessfulPUTResponse(useVersioning);
			}

			@Override
			public Boolean rollback(PutObjectResponseType arg)
					throws S3Exception, Exception {
				return true;
			}
			
		};
		
		try {
			//Populate a bunch of fake object entities.
			for(int i = 0 ; i < entityCount ; i++) {
				testEntity = generateFakeValidEntity(bucketName, key + String.valueOf(i), false);
				testEntities.add(testEntity);
				ObjectManagers.getInstance().create(bucketName, testEntity, fakeModifier);
			}
						
			PaginatedResult<ObjectEntity> r = ObjectManagers.getInstance().listPaginated(bucketName, 100, null, null, null);
			
			for(ObjectEntity e : r.getEntityList()) {
				System.out.println(e.toString());
			}
			
			Assert.assertTrue(r.getEntityList().size() == entityCount);
				
		} catch(Exception e) {
			LOG.error("Transaction error", e);
			Assert.fail("Failed getting listing");
			
		} finally {
			for(ObjectEntity obj : testEntities) {
				try {
					ObjectManagers.getInstance().delete(obj, null);
				} catch(Exception e) {
					LOG.error("Error deleteing entity: " + obj.toString(), e);
				}
			}
		}
	}
	
	@Test
	public void testPaginatedListing() {
		Assert.fail("Not implemented");
	}
	
	
	/*
	 * Tests create, lookup, delete lifecycle a single object
	 */
	@Test
	public void testBasicLifecycle() {
		String bucket = "testbucket";
		String key = "testkey";
		String versionId = null;
		String requestId = UUID.randomUUID().toString();
		long contentLength = 100;
		final boolean useVersioning = false;
		
		User usr = Principals.systemUser();
		ObjectEntity object1 = new ObjectEntity();
		try {
			object1.initializeForCreate(bucket, key, versionId, requestId, contentLength, usr);
		} catch(Exception e) {
			LOG.error(e);
		}
		
		CallableWithRollback<PutObjectResponseType, Boolean> fakeModifier = new CallableWithRollback<PutObjectResponseType, Boolean>() {

			@Override
			public PutObjectResponseType call() throws S3Exception, Exception {
				return getFakeSuccessfulPUTResponse(useVersioning);
			}

			@Override
			public Boolean rollback(PutObjectResponseType arg)
					throws S3Exception, Exception {
				return true;
			}
			
		};
		try {
			ObjectManagers.getInstance().create(bucket, object1, fakeModifier);
			
			Assert.assertTrue(ObjectManagers.getInstance().exists(bucket, key, null, null));
			
			ObjectEntity object2 = ObjectManagers.getInstance().get(bucket, key, null);			
			Assert.assertTrue(object2.equals(object1));
			
			ObjectManagers.getInstance().delete(object2, null);			
			Assert.assertFalse(ObjectManagers.getInstance().exists(bucket, key, null, null));			
		} catch(Exception e) {
			LOG.error(e);
		}		
	}
	
	public void testCount() {
		Assert.fail("Not implemented");
	}
}
