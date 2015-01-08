/*
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
 */

package com.eucalyptus.objectstorage.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.objectstorage.UnitTestSupport;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.storage.msgs.s3.*;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.Permission;

public class AclTests {
	public static Grant PUBLIC_READ_GRANT = null;
	public static Grant PUBLIC_READ_WRITE_GRANT = null;
	public static Grant AUTHENTICATED_READ_GRANT = null;
	public static Grant PRIVATE_GRANT = null;
	public static Grant AWS_EXEC_READ_GRANT = null;
	public static Grant LOG_DELIVERY_GRANT = null;
	public static Grant BUCKET_OWNER_READ_GRANT = null;
	public static Grant BUCKET_OWNER_FULL_CONTROL_GRANT = null;

	public static String account1CanonicalId;
	public static CanonicalUser canonicalUser1;
	public static Grantee account1;
	public static Grant account1Read;
	public static Grant account1Write;
	public static Grant account1FullControl;
	public static Grant account1ReadAcp;
	public static Grant account1WriteAcp;

	
	public static String account2CanonicalId;
	public static CanonicalUser canonicalUser2;
	public static Grantee account2;
	public static Grant account2Read;
	public static Grant account2Write;
	public static Grant account2FullControl;
	public static Grant account2ReadAcp;
	public static Grant account2WriteAcp;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
        UnitTestSupport.setupAuthPersistenceContext();
        UnitTestSupport.initializeAuth(2, 2);

        Iterator<String> accountNameIterator = UnitTestSupport.getTestAccounts().iterator();
        account1CanonicalId = Accounts.lookupAccountByName(accountNameIterator.next()).getCanonicalId();
        canonicalUser1 = new CanonicalUser(account1CanonicalId, Accounts.lookupAccountByCanonicalId(account1CanonicalId).getName());
        account1 = new Grantee(canonicalUser1);
        account1Read = new Grant(account1, Permission.READ.toString());
        account1Write = new Grant(account1, Permission.WRITE.toString());
        account1FullControl = new Grant(account1, Permission.FULL_CONTROL.toString());
        account1ReadAcp = new Grant(account1, Permission.READ_ACP.toString());
        account1WriteAcp = new Grant(account1, Permission.WRITE_ACP.toString());

        account2CanonicalId = Accounts.lookupAccountByName(accountNameIterator.next()).getCanonicalId();
        canonicalUser2 = new CanonicalUser(account2CanonicalId, Accounts.lookupAccountByCanonicalId(account2CanonicalId).getName());
        account2 = new Grantee(canonicalUser2);
        account2Read = new Grant(account2, Permission.READ.toString());
        account2Write = new Grant(account2, Permission.WRITE.toString());
        account2FullControl = new Grant(account2, Permission.FULL_CONTROL.toString());
        account2ReadAcp = new Grant(account2, Permission.READ_ACP.toString());
        account2WriteAcp = new Grant(account2, Permission.WRITE_ACP.toString());

        Accounts.addSystemAccount().addUser(User.ACCOUNT_ADMIN, "/", true, null);
    }

	@Test
	public void testCannedAclExpansion() throws Exception {
		String bucketCanonicalId = account1CanonicalId;
		String objectCanonicalId = account1CanonicalId;
        HashMap<AccessControlList, AccessControlList> testPolicyMap = new HashMap<AccessControlList, AccessControlList>();
        HashMap<ObjectStorageProperties.CannedACL, AccessControlList> realAclMap = new HashMap<ObjectStorageProperties.CannedACL, AccessControlList>();
        HashMap<ObjectStorageProperties.CannedACL, AccessControlList> msgAclMap = new HashMap<ObjectStorageProperties.CannedACL, AccessControlList>();

        //Manually construct the ACLs for the canned acl strings.
        AccessControlList realAcl;

        //Private
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.private_only, realAcl);

        //Public-Read
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.toString())), Permission.READ.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.public_read, realAcl);

        //Public-Read-Write
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.toString())), Permission.WRITE.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.toString())), Permission.READ.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.public_read_write, realAcl);


        //Aws-exec-read
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ.toString())), Permission.READ.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.aws_exec_read, realAcl);

        //Authenticated-Read
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP.toString())), Permission.READ.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.authenticated_read, realAcl);

        //Bucket-Owner-Read
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(bucketCanonicalId, "")), Permission.READ.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_read, realAcl);

        //Bucket-Owner-Full-Control
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(bucketCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_full_control, realAcl);

        //Log-delivery-write
        realAcl = new AccessControlList();
        realAcl.setGrants(new ArrayList<Grant>());
        realAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(objectCanonicalId, "")), Permission.FULL_CONTROL.toString()));
        realAcl.getGrants().add(new Grant(new Grantee(new Group(ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString())), Permission.WRITE.toString()));
        realAclMap.put(ObjectStorageProperties.CannedACL.log_delivery_write, realAcl);


        //Populate the input-output map for verification
        for(ObjectStorageProperties.CannedACL cannedAcl : ObjectStorageProperties.CannedACL.values()) {
            AccessControlList fakeAcl= new AccessControlList();
            fakeAcl.setGrants(new ArrayList<Grant>());
            fakeAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser("","")), cannedAcl.toString()));
            msgAclMap.put(cannedAcl, fakeAcl);
        }

        //Ensure the expansion check correctly fails on incompatible acls.
        assert(!checkExpansion(AclUtils.expandCannedAcl(msgAclMap.get(ObjectStorageProperties.CannedACL.private_only), bucketCanonicalId, objectCanonicalId), realAclMap.get(ObjectStorageProperties.CannedACL.bucket_owner_full_control)));

		for(ObjectStorageProperties.CannedACL cannedAcl : ObjectStorageProperties.CannedACL.values()) {
			try {
                System.out.println("Checking expansion of canned acl: " + cannedAcl.toString());
                AccessControlList list = AclUtils.expandCannedAcl(msgAclMap.get(cannedAcl), bucketCanonicalId, objectCanonicalId);
                assert(checkExpansion(list, realAclMap.get(cannedAcl)));
                System.out.println("Expansion of canned acl: " + cannedAcl.toString() + " passed with: " + list.toString());
			} catch(Exception e) {
				e.printStackTrace();
                fail("Exception in expansion or check for acl " + cannedAcl.toString() + " with error "  + e.getMessage());
			}
		}

	}
	
	/**
	 * Check using permission check, not structure itself
	 * @return
	 */
	boolean checkExpansion(AccessControlList test, AccessControlList expected) {
        //Does an unordered comparison of set equality.
		return test.equals(expected);
	}
	
	@Test	
	public void testACLGroupMembership() throws Exception {
		String anonymous = Principals.nobodyUser().getUserId();
		String admin = Principals.systemUser().getUserId();
        String eucaAdmin = Accounts.lookupSystemAdmin().getUserId();
        String random = "01231295135";


        Assert.assertTrue("Anonymous should be in AllUsers", AclUtils.isUserMember(anonymous, ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
        Assert.assertTrue("Anonymous should NOT be in AuthenticatedUsers", !AclUtils.isUserMember(anonymous, ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
        Assert.assertTrue("Anonymous should NOT be in AWS-Exec", !AclUtils.isUserMember(anonymous, ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
        Assert.assertTrue("Anonymous should NOT be in EC2-Bundle-Read", !AclUtils.isUserMember(anonymous, ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ));
        Assert.assertTrue("Anonymous should NOT be in Logging-group", !AclUtils.isUserMember(anonymous, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));


        Assert.assertTrue("System should be in AllUsers", AclUtils.isUserMember(admin, ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
        Assert.assertTrue("System should be in AuthenticatedUsers", AclUtils.isUserMember(admin, ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
        Assert.assertTrue("System should be in AWS-Exec", AclUtils.isUserMember(admin, ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
        Assert.assertTrue("System should be in EC2-Bundle-Read", AclUtils.isUserMember(admin, ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ));
		Assert.assertTrue("System should be in Logging-group", AclUtils.isUserMember(admin, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));

        Assert.assertTrue("Euca/Admin should be in AllUsers", AclUtils.isUserMember(eucaAdmin, ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
        Assert.assertTrue("Euca/Admin should be in AuthenticatedUsers", AclUtils.isUserMember(eucaAdmin, ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
        Assert.assertTrue("Euca/Admin should be in AWS-Exec", AclUtils.isUserMember(eucaAdmin, ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
        Assert.assertTrue("Euca/Admin should be in EC2-Bundle-Read", AclUtils.isUserMember(eucaAdmin, ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ));
        Assert.assertTrue("Euca/Admin should be in Logging-group", AclUtils.isUserMember(eucaAdmin, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));

        Assert.assertTrue("Random should be in AllUsers", AclUtils.isUserMember(random, ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP));
        Assert.assertTrue("Random should be in AuthenticatedUsers", AclUtils.isUserMember(random, ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP));
        Assert.assertTrue("Random should NOT be in AWS-Exec", !AclUtils.isUserMember(random, ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ));
        Assert.assertTrue("Random should NOT be in EC2-Bundle-Read", !AclUtils.isUserMember(random, ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ));
        Assert.assertTrue("Random should NOT be in Logging-group", !AclUtils.isUserMember(random, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP));

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

        assert(!account1CanonicalId.equals(account2CanonicalId));

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
            fail(e.toString() + ": " + e.getMessage());
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

		acl.getGrants().add(account2Read);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
            fail(e.toString() + ": " + e.getMessage());
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

		acl.getGrants().add(account2ReadAcp);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
            fail(e.toString() + ": " + e.getMessage());
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
		

		acl.getGrants().add(account2Write);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
            fail(e.toString() + ": " + e.getMessage());
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
		

		acl.getGrants().add(account2WriteAcp);
		try {
			testEntity.setAcl(acp);
		} catch (Exception e) {
            fail(e.toString() + ": " + e.getMessage());
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
		//FULL_CONTROL granted incrementally, should be equivalent now
        Assert.assertTrue("User2 should be able to full control", testEntity.can(Permission.FULL_CONTROL, account2CanonicalId));
		
	}

    @Test
    public void testAcpGeneration() throws Exception {
        String canonicalId = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().iterator().next()).getCanonicalId();
        User user = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().iterator().next()).get(0));

        AccessControlList acl = new AccessControlList();
        AccessControlPolicy acp = new AccessControlPolicy();
        acp.setAccessControlList(acl);

        AccessControlPolicy genAcp = AclUtils.processNewResourcePolicy(user, acp, canonicalId);

        assert(genAcp != null);
        assert(genAcp.getAccessControlList() != null);
        assert(genAcp.getOwner() != null);
        assert(genAcp.getOwner().getID() != null);
        assert(genAcp.getAccessControlList().getGrants() != null);
        assert(genAcp.getAccessControlList().getGrants().size() == 1);
        assert(genAcp.getAccessControlList().getGrants().get(0).getPermission().equals(Permission.FULL_CONTROL));
        assert(genAcp.getAccessControlList().getGrants().get(0).getGrantee().getCanonicalUser().getID().equals(canonicalId));
        AccessControlPolicy genAcp2 = AclUtils.processNewResourcePolicy(user, new AccessControlPolicy(), canonicalId);

        assert(genAcp2 != null);
        assert(genAcp2.getAccessControlList() != null);
        assert(genAcp2.getAccessControlList().getGrants().get(0).getPermission().equals(Permission.FULL_CONTROL));
        assert(genAcp2.getAccessControlList().getGrants().get(0).getGrantee().getCanonicalUser().getID().equals(canonicalId));
        assert(genAcp2.getOwner() != null);
        assert(genAcp2.getOwner().getID() != null);
        assert(genAcp2.getAccessControlList().getGrants() != null);

        CanonicalUser aws = new CanonicalUser();
        aws.setDisplayName("");
        Grant grant = new Grant(new Grantee(aws), "private");
        AccessControlList cannedAcl = new AccessControlList();
        cannedAcl.getGrants().add(grant);

        AccessControlPolicy genAcp3 = AclUtils.processNewResourcePolicy(user, new AccessControlPolicy(), canonicalId);

        assert(genAcp3 != null);
        assert(genAcp3.getAccessControlList() != null);
        assert(genAcp3.getAccessControlList().getGrants().get(0).getPermission().equals(Permission.FULL_CONTROL));
        assert(genAcp3.getAccessControlList().getGrants().get(0).getGrantee().getCanonicalUser().getID().equals(canonicalId));
        assert(genAcp3.getOwner() != null);
        assert(genAcp3.getOwner().getID() != null);
        assert(genAcp3.getAccessControlList().getGrants() != null);

    }

    @Test
    public void testAcpGenerationFailWithNulls() throws Exception {
        String canonicalId = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().iterator().next()).getCanonicalId();
        User user = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().iterator().next()).get(0));

        AccessControlList acl = new AccessControlList();
        AccessControlPolicy acp = new AccessControlPolicy();
        acp.setAccessControlList(acl);

        AccessControlPolicy genAcp = AclUtils.processNewResourcePolicy(user, acp, canonicalId);
        assert(validateAcp(genAcp, canonicalId, Permission.FULL_CONTROL));

        AccessControlPolicy genAcp2 = AclUtils.processNewResourcePolicy(user, new AccessControlPolicy(), canonicalId);
        assert(validateAcp(genAcp2, canonicalId, Permission.FULL_CONTROL));

        try {
            genAcp = AclUtils.processNewResourcePolicy(null, null, null);
            fail("Should have gotten exception on policy gen with all nulls");
        } catch(Exception e) {
            System.out.println("Correctly caught the exception on all nulls for policy gen: " + e.getMessage());
        }

        genAcp = AclUtils.processNewResourcePolicy(user, null, null);
        assert(validateAcp(genAcp, canonicalId, Permission.FULL_CONTROL));

        genAcp = AclUtils.processNewResourcePolicy(user, acp, null);
        assert(validateAcp(genAcp, canonicalId, Permission.FULL_CONTROL));

        genAcp = AclUtils.processNewResourcePolicy(user, new AccessControlPolicy(), canonicalId);
        assert(validateAcp(genAcp, canonicalId, Permission.FULL_CONTROL));

    }

    @Test
    public void testAcpGenerationForDeleteMarkers() throws Exception {
        String canonicalId = Accounts.lookupAccountByName(UnitTestSupport.getTestAccounts().iterator().next()).getCanonicalId();
        User user = Accounts.lookupUserById(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().iterator().next()).get(0));

        AccessControlList acl = new AccessControlList();
        AccessControlPolicy acp = new AccessControlPolicy();
        acp.setAccessControlList(acl);

        AccessControlPolicy genAcp = AclUtils.processNewResourcePolicy(user, null, canonicalId);
        assert(validateAcp(genAcp, canonicalId, Permission.FULL_CONTROL));

        try {
            genAcp = AclUtils.processNewResourcePolicy(null, null, null);
            fail("Should have gotten exception on policy gen with all nulls");
        } catch(Exception e) {
            System.out.println("Correctly caught the exception on all nulls for policy gen: " + e.getMessage());
        }

    }

    private static boolean validateAcp(AccessControlPolicy acp, String expectedOwnerCanonicalId, Permission expectedPermission) {
        return(acp != null &&
                acp.getAccessControlList() != null &&
                acp.getAccessControlList().getGrants() != null &&
                acp.getOwner() != null &&
                !Strings.isNullOrEmpty(acp.getOwner().getID()) &&
                acp.getOwner().getID().equals(expectedOwnerCanonicalId) &&
                acp.getAccessControlList().getGrants().size() == 1 &&
                acp.getAccessControlList().getGrants().get(0).getPermission().equals(expectedPermission.toString()) &&
                acp.getAccessControlList().getGrants().get(0).getGrantee().getCanonicalUser().getID().equals(expectedOwnerCanonicalId)
        );
    }
}
