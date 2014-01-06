package com.eucalyptus.objectstorage;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.Permission;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.google.gwt.dev.util.collect.HashMap;

public class AclTests {
	public static Grant PUBLIC_READ_GRANT = null;
	public static Grant PUBLIC_READ_WRITE_GRANT = null;
	public static Grant AUTHENTICATED_READ_GRANT = null;	
	public static Grant PRIVATE_GRANT = null;
	public static Grant AWS_EXEC_READ_GRANT = null;
	public static Grant LOG_DELIVERY_GRANT = null;
	public static Grant BUCKET_OWNER_READ_GRANT = null;
	public static Grant BUCKET_OWNER_FULL_CONTROL_GRANT = null;
	
	public static String account1CanonicalId = "123abc";
	public static CanonicalUser canonicalUser1 = new CanonicalUser(account1CanonicalId,"account1");
	public static Grantee account1 = new Grantee(canonicalUser1);	
	public static Grant account1Read = new Grant(account1, Permission.READ.toString());
	public static Grant account1Write = new Grant(account1, Permission.WRITE.toString());
	public static Grant account1FullControl = new Grant(account1, Permission.FULL_CONTROL.toString());
	public static Grant account1ReadAcp = new Grant(account1, Permission.READ_ACP.toString());
	public static Grant account1WriteAcp = new Grant(account1, Permission.WRITE_ACP.toString());
	
	
	public static String account2CanonicalId = "456def";
	public static CanonicalUser canonicalUser2 = new CanonicalUser(account2CanonicalId,"account2");	
	public static Grantee account2 = new Grantee(canonicalUser2);	
	public static Grant account2Read = new Grant(account2, Permission.READ.toString());
	public static Grant account2Write = new Grant(account2, Permission.WRITE.toString());
	public static Grant account2FullControl = new Grant(account2, Permission.FULL_CONTROL.toString());
	public static Grant account2ReadAcp = new Grant(account2, Permission.READ_ACP.toString());
	public static Grant account2WriteAcp = new Grant(account2, Permission.WRITE_ACP.toString());
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception { }

	@Before
	public void setUp() throws Exception { }

	@After
	public void tearDown() throws Exception { }

	@Ignore
	@Test
	public void testCannedAclExpansion() {
		String bucketCanonicalId = "12o391aahabadosidvawerg-bucket";
		String objectCanonicalId = "1asfasf2o391aosidvawerg-object";
		HashMap<AccessControlList, AccessControlList> testPolicyMap = new HashMap<AccessControlList, AccessControlList>();
		
		AccessControlList acl = new AccessControlList();
		acl.setGrants(new ArrayList<Grant>());
		boolean doFail = false;
		
		
		for(Map.Entry<AccessControlList, AccessControlList> entry : testPolicyMap.entrySet()) {
			try { 
				Assert.assertTrue("Expansion check failed", checkExpansion(AclUtils.expandCannedAcl(entry.getKey(), bucketCanonicalId, objectCanonicalId), entry.getValue()));
			} catch(Exception e) {
				System.err.println("Error in check: " + e.getMessage());
				e.printStackTrace();
				doFail = true;
			}
		}
		
		if(doFail) fail();
		
	}
	
	/**
	 * Check using permission check, not structure itself
	 * @return
	 */
	boolean checkExpansion(AccessControlList test, AccessControlList expected) {
		//will fail until this operator is fixed in the types
		return test.equals(expected);
	}
	
	@Test	
	public void testACLGroupMembership() {
		User anonymous = Principals.nobodyUser();
		User admin = Principals.systemUser();
		
		Assert.assertTrue("Anonymous should be in AllUsers", AclUtils.isUserMember(anonymous.getUserId(), ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
		Assert.assertTrue("Anonymous should not be in AuthenticatedUsers", !AclUtils.isUserMember(anonymous.getUserId(), ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
		Assert.assertTrue("System should be in AllUsers", AclUtils.isUserMember(admin.getUserId(), ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
		Assert.assertTrue("System should be in AuthenticatedUsers", AclUtils.isUserMember(admin.getUserId(), ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
		
		Assert.assertTrue("System should be in AWS-Exec", AclUtils.isUserMember(admin.getUserId(), ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
		Assert.assertTrue("System should be in Logging-group", AclUtils.isUserMember(admin.getUserId(), ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));
		
		Assert.assertTrue("Random should NOT be in AWS-Exec", !AclUtils.isUserMember("01231295135", ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
		Assert.assertTrue("Random shoudl NOT be in Logging-group", !AclUtils.isUserMember("12523957135", ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));

	}
	
	private S3AccessControlledEntity getACLEntity(final String name) {
		S3AccessControlledEntity testEntity = new S3AccessControlledEntity() {
			public String getResourceFullName() {
				return name;
			}
		};
		
		return testEntity;		
	}
	
	@Test
	public void testPermissionChecks() {
		S3AccessControlledEntity testEntity = getACLEntity("");
		
		testEntity.setOwnerCanonicalId(account1CanonicalId);
		AccessControlList acl = new AccessControlList();
		acl.setGrants(new ArrayList<Grant>());
		acl.getGrants().add(account1FullControl);
		
		
		AccessControlPolicy acp = new AccessControlPolicy();
		acp.setOwner(canonicalUser1);
		acp.setAccessControlList(acl);		
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
			System.err.println("Error setting acp");
			e.printStackTrace();
			fail();
		}
		
		System.out.println("Testing acp for user1. User1 only in acl");
		Assert.assertTrue("User1 should be able to read", testEntity.can(Permission.READ, account1CanonicalId));
		Assert.assertTrue("User1 should be able to read acp", testEntity.can(Permission.READ_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write", testEntity.can(Permission.WRITE, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account1CanonicalId));
		
		System.out.println("Testing acp for user2. User1 only in acl, should not be allowed");
		Assert.assertTrue("User2 should not be able to read", !testEntity.can(Permission.READ, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to read acp", !testEntity.can(Permission.READ_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write", !testEntity.can(Permission.WRITE, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write acp", !testEntity.can(Permission.WRITE_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to full control", !testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));

		System.out.println("Addind account2 read access");
		acl.getGrants().add(account2Read);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
			System.err.println("Error setting acp");
			e.printStackTrace();
			fail();
		}
		
		Assert.assertTrue("User1 should be able to read", testEntity.can(Permission.READ, account1CanonicalId));
		Assert.assertTrue("User1 should be able to read acp", testEntity.can(Permission.READ_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write", testEntity.can(Permission.WRITE, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account1CanonicalId));
		
		Assert.assertTrue("User2 should be able to read", testEntity.can(Permission.READ, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to read acp", !testEntity.can(Permission.READ_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write", !testEntity.can(Permission.WRITE, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write acp", !testEntity.can(Permission.WRITE_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to full control", !testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));


		System.out.println("Addind account2 read_acp access");
		acl.getGrants().add(account2ReadAcp);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
			System.err.println("Error setting acp");
			e.printStackTrace();
			fail();
		}
		
		Assert.assertTrue("User1 should be able to read", testEntity.can(Permission.READ, account1CanonicalId));
		Assert.assertTrue("User1 should be able to read acp", testEntity.can(Permission.READ_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write", testEntity.can(Permission.WRITE, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account1CanonicalId));
		
		Assert.assertTrue("User2 should be able to read", testEntity.can(Permission.READ, account2CanonicalId));
		Assert.assertTrue("User2 should be able to read acp", testEntity.can(Permission.READ_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write", !testEntity.can(Permission.WRITE, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write acp", !testEntity.can(Permission.WRITE_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to full control", !testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));
		

		System.out.println("Addind account2 write access");
		acl.getGrants().add(account2Write);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
			System.err.println("Error setting acp");
			e.printStackTrace();
			fail();
		}
		
		Assert.assertTrue("User1 should be able to read", testEntity.can(Permission.READ, account1CanonicalId));
		Assert.assertTrue("User1 should be able to read acp", testEntity.can(Permission.READ_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write", testEntity.can(Permission.WRITE, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account1CanonicalId));
		
		Assert.assertTrue("User2 should be able to read", testEntity.can(Permission.READ, account2CanonicalId));
		Assert.assertTrue("User2 should be able to read acp", testEntity.can(Permission.READ_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should be able to write", testEntity.can(Permission.WRITE, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to write acp", !testEntity.can(Permission.WRITE_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to full control", !testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));
		

		System.out.println("Addind account2 write_acp access");
		acl.getGrants().add(account2WriteAcp);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
			System.err.println("Error setting acp");
			e.printStackTrace();
			fail();
		}
		
		Assert.assertTrue("User1 should be able to read", testEntity.can(Permission.READ, account1CanonicalId));
		Assert.assertTrue("User1 should be able to read acp", testEntity.can(Permission.READ_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write", testEntity.can(Permission.WRITE, account1CanonicalId));
		Assert.assertTrue("User1 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account1CanonicalId));
		Assert.assertTrue("User1 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account1CanonicalId));
		
		Assert.assertTrue("User2 should be able to read", testEntity.can(Permission.READ, account2CanonicalId));
		Assert.assertTrue("User2 should be able to read acp", testEntity.can(Permission.READ_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should be able to write", testEntity.can(Permission.WRITE, account2CanonicalId));
		Assert.assertTrue("User2 should be able to write acp", testEntity.can(Permission.WRITE_ACP, account2CanonicalId));
		Assert.assertTrue("User2 should not be able to full control", !testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));		
		
	}

}
