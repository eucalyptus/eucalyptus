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

package com.eucalyptus.objectstorage.util;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Maps CannedAcls to access control lists
 */
public class AclUtils {
    private static final Logger LOG = Logger.getLogger(AclUtils.class);

    /* Use string as key since enum doesn't allow '-' which is in the ACL types. Allows a direct lookup from the msg */
    private static final HashMap<String, Function<OwnerIdPair, List<Grant>>> cannedAclMap = new HashMap<String, Function<OwnerIdPair, List<Grant>>>();

    //A lookup map for quick verification of group uris
    private static final HashMap<String, ObjectStorageProperties.S3_GROUP> groupUriMap = new HashMap<String, ObjectStorageProperties.S3_GROUP>();

    static {
        //Populate the map
        cannedAclMap.put(ObjectStorageProperties.CannedACL.private_only.toString(), PrivateOnlyGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.authenticated_read.toString(), AuthenticatedReadGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.public_read.toString(), PublicReadGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.public_read_write.toString(), PublicReadWriteGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.aws_exec_read.toString(), AwsExecReadGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_full_control.toString(), BucketOwnerFullControlGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_read.toString(), BucketOwnerReadGrantBuilder.INSTANCE);
        cannedAclMap.put(ObjectStorageProperties.CannedACL.log_delivery_write.toString(), LogDeliveryWriteGrantBuilder.INSTANCE);

        for (ObjectStorageProperties.S3_GROUP g : ObjectStorageProperties.S3_GROUP.values()) {
            groupUriMap.put(g.toString(), g);
        }
    }

    /*
     * Simply determines if the userId is a member of the groupId, very simplistic only for ALL_USERS and AUTHENTICATED_USERS, not arbitrary groups.
     * Arbitrary groups are not yet supported in ObjectStorage bucket policies/IAM policies.
     * userId should be a canonicalId
     */
    public static boolean isUserMemberOfGroup(String userId, String groupId) {
        if (Strings.isNullOrEmpty(groupId)) {
            return false;
        }

        try {
            ObjectStorageProperties.S3_GROUP group = groupUriMap.get(groupId);
            return group != null && isUserMember(userId, group);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown group id requested for membership check: " + groupId);
            return false;
        }
    }

    /**
     * Just checks the basic S3 groups for membership of the userId.
     * Caller must ensure that the userId is a valid ID in the system. That is outside the scope of this method.
     *
     * @param userId
     * @param group
     * @return
     */
    public static boolean isUserMember(String userId, ObjectStorageProperties.S3_GROUP group) {
        if (group == null) {
            return false;
        }

        if (ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.equals(group)) {
            return true;
        }

        if (ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP.equals(group)
                && !Strings.isNullOrEmpty(userId) && !userId.equals(Principals.nobodyUser().getUserId())) {
            return true;
        }

        //System only in the aws-exec-read group (zateam)
        if (ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ.equals(group) &&
                Principals.systemUser().getUserId().equals(userId)) {
            return true;
        }

        //System only in logging
        if (ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.equals(group) &&
                Principals.systemUser().getUserId().equals(userId)) {
            return true;
        }

        return false;
    }

    /**
     * Utility class for passing pairs of canonicalIds around without using
     * something ambiguous like an String-array.
     *
     * @author zhill
     */
    public static class OwnerIdPair {
        private String bucketOwner;
        private String objectOwner;

        public OwnerIdPair(String bucketOwnerCanonicalId, String objectOwnerCanonicalId) {
            this.bucketOwner = bucketOwnerCanonicalId;
            this.objectOwner = objectOwnerCanonicalId;
        }

        public String getBucketOwnerCanonicalId() {
            return this.bucketOwner;
        }

        public String getObjectOwnerCanonicalId() {
            return this.objectOwner;
        }
    }

    /**
     * If the object ownerId is set in the OwnerIdPair then this will assume that
     * the resource is an object and will return a full-control grant for that user.
     * If bucket owner only is set then this assumes that the bucket is the resource.
     *
     * @author zhill
     */
    protected enum PrivateOnlyGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            ArrayList<Grant> privateGrants = new ArrayList<Grant>();
            Grant ownerFullControl = new Grant();
            Grantee owner = new Grantee();
            String displayName = "";
            String ownerCanonicalId = null;
            if (!Strings.isNullOrEmpty(ownerIds.getObjectOwnerCanonicalId())) {
                ownerCanonicalId = ownerIds.getObjectOwnerCanonicalId();
            } else {
                ownerCanonicalId = ownerIds.getBucketOwnerCanonicalId();
            }

            try {
                displayName = Accounts.lookupAccountByCanonicalId(ownerCanonicalId).getName();
            } catch (AuthException e) {
                displayName = "";
            }
            owner.setCanonicalUser(new CanonicalUser(ownerCanonicalId, displayName));
            owner.setType("CanonicalUser");
            ownerFullControl.setGrantee(owner);
            ownerFullControl.setPermission(ObjectStorageProperties.Permission.FULL_CONTROL.toString());
            privateGrants.add(ownerFullControl);
            return privateGrants;
        }
    }

    ;

    protected enum AuthenticatedReadGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> authenticatedRead = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            Grantee authenticatedUsers = new Grantee();
            authenticatedUsers.setGroup(new Group(ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP.toString()));
            Grant authUsersGrant = new Grant();
            authUsersGrant.setPermission(ObjectStorageProperties.Permission.READ.toString());
            authUsersGrant.setGrantee(authenticatedUsers);
            authenticatedRead.add(authUsersGrant);
            return authenticatedRead;
        }
    }

    ;

    protected enum PublicReadGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> publicRead = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            Grantee allUsers = new Grantee();
            allUsers.setGroup(new Group(ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.toString()));
            Grant allUsersGrant = new Grant();
            allUsersGrant.setPermission(ObjectStorageProperties.Permission.READ.toString());
            allUsersGrant.setGrantee(allUsers);
            publicRead.add(allUsersGrant);
            return publicRead;
        }
    }

    ;

    protected enum PublicReadWriteGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> publicReadWrite = PublicReadGrantBuilder.INSTANCE.apply(ownerIds);
            Grantee allUsers = new Grantee();
            allUsers.setGroup(new Group(ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.toString()));
            Grant allUsersGrant = new Grant();
            allUsersGrant.setPermission(ObjectStorageProperties.Permission.WRITE.toString());
            allUsersGrant.setGrantee(allUsers);
            publicReadWrite.add(allUsersGrant);
            return publicReadWrite;
        }
    }

    ;

    protected enum AwsExecReadGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> awsExecRead = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            Grantee execReadGroup = new Grantee();
            execReadGroup.setGroup(new Group(ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ.toString()));
            Grant execReadGrant = new Grant();
            execReadGrant.setPermission(ObjectStorageProperties.Permission.READ.toString());
            execReadGrant.setGrantee(execReadGroup);
            awsExecRead.add(execReadGrant);
            return awsExecRead;
        }
    }

    ;

    protected enum BucketOwnerFullControlGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> bucketOwnerFullControl = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            String canonicalId = ownerIds.getBucketOwnerCanonicalId();
            String displayName = "";
            try {
                displayName = Accounts.lookupAccountByCanonicalId(canonicalId).getName();
            } catch (AuthException e) {
                displayName = "";
            }

            Grantee bucketOwner = new Grantee();
            bucketOwner.setCanonicalUser(new CanonicalUser(canonicalId, displayName));
            Grant bucketOwnerGrant = new Grant();
            bucketOwnerGrant.setPermission(ObjectStorageProperties.Permission.FULL_CONTROL.toString());
            bucketOwnerGrant.setGrantee(bucketOwner);
            bucketOwnerFullControl.add(bucketOwnerGrant);
            return bucketOwnerFullControl;
        }
    }

    ;

    protected enum BucketOwnerReadGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> bucketOwnerRead = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            String canonicalId = ownerIds.getBucketOwnerCanonicalId();
            String displayName = "";
            try {
                displayName = Accounts.lookupAccountByCanonicalId(canonicalId).getName();
            } catch (AuthException e) {
                displayName = "";
            }

            Grantee bucketOwner = new Grantee();
            bucketOwner.setCanonicalUser(new CanonicalUser(canonicalId, displayName));
            Grant bucketOwnerGrant = new Grant();
            bucketOwnerGrant.setPermission(ObjectStorageProperties.Permission.READ.toString());
            bucketOwnerGrant.setGrantee(bucketOwner);
            bucketOwnerRead.add(bucketOwnerGrant);
            return bucketOwnerRead;
        }
    }

    ;

    protected enum LogDeliveryWriteGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
        INSTANCE;

        @Override
        public List<Grant> apply(OwnerIdPair ownerIds) {
            List<Grant> logDeliveryWrite = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
            Grantee logGroup = new Grantee();
            logGroup.setGroup(new Group(ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString()));

            Grant loggingWriteGrant = new Grant();
            loggingWriteGrant.setPermission(ObjectStorageProperties.Permission.WRITE.toString());
            loggingWriteGrant.setGrantee(logGroup);

            Grant loggingReadAcpGrant = new Grant();
            loggingReadAcpGrant.setPermission(ObjectStorageProperties.Permission.READ_ACP.toString());
            loggingReadAcpGrant.setGrantee(logGroup);

            logDeliveryWrite.add(loggingWriteGrant);
            logDeliveryWrite.add(loggingReadAcpGrant);
            return logDeliveryWrite;
        }
    }

    ;

    public static ObjectStorageProperties.S3_GROUP getGroupFromUri(String uri) throws NoSuchElementException {
        ObjectStorageProperties.S3_GROUP foundGroup = groupUriMap.get(uri);
        if (foundGroup == null) {
            throw new NoSuchElementException(uri);
        }
        return foundGroup;
    }

    /**
     * Processes a list by finding all canned-acls and expanding those.
     * The returned list is a new list that includes all non-canned ACL entries
     * of the input as well as the expanded grants mapped to canned-acls
     * <p/>
     * CannedAcls are Grants with Grantee = "", and Permision is the canned-acl string
     *
     * @param msgAcl
     * @return
     */
    public static AccessControlList expandCannedAcl(@Nonnull AccessControlList msgAcl, @Nullable final String bucketOwnerCanonicalId, @Nullable final String objectOwnerCanonicalId) throws EucalyptusCloudException {
        if (msgAcl == null) {
            throw new IllegalArgumentException("Null list received");
        }

        AccessControlList outputList = new AccessControlList();
        if (outputList.getGrants() == null) {
            //Should be handled by constructor of ACL, but just to be sure
            outputList.setGrants(new ArrayList<Grant>());
        }
        final OwnerIdPair owners = new OwnerIdPair(bucketOwnerCanonicalId, objectOwnerCanonicalId);
        String entryValue = null;
        for (Grant msgGrant : msgAcl.getGrants()) {
            entryValue = msgGrant.getPermission(); //The OSG binding populates the canned-acl in the permission field
            try {
                if (cannedAclMap.containsKey(entryValue)) {
                    outputList.getGrants().addAll(cannedAclMap.get(entryValue).apply(owners));
                } else {
                    //add to output.
                    outputList.getGrants().add(msgGrant);
                }
            } catch (Exception e) {
                //Failed. Stop now
                throw new EucalyptusCloudException("Failed generating the full ACL from canned ACL", e);
            }
        }
        return outputList;
    }

    public static CanonicalUser buildCanonicalUser(Account accnt) {
        return new CanonicalUser(accnt.getCanonicalId(), accnt.getName());
    }

    /**
     * Ensures the the policy is not empty. If found empty or null, a 'private' policy is generated and returned.
     * If creating for an object, the BucketOwnerCanonicalId must not be null. If found null, then a bucket-creation is
     * expected and ACLs will be expanded as such.
     *
     * @param requestUser
     * @param policy
     * @return
     */
    public static AccessControlPolicy processNewResourcePolicy(@Nonnull User requestUser, @Nullable AccessControlPolicy policy, @Nullable String bucketOwnerCanonicalId) throws Exception {
        AccessControlPolicy acPolicy = null;
        if (policy != null) {
            acPolicy = policy;
        } else {
            acPolicy = new AccessControlPolicy();
        }

        if (acPolicy.getOwner() == null) {
            acPolicy.setOwner(buildCanonicalUser(requestUser.getAccount()));
        }

        if (acPolicy.getAccessControlList() == null) {
            acPolicy.setAccessControlList(new AccessControlList());
        }

        if (acPolicy.getAccessControlList().getGrants() == null ||
                acPolicy.getAccessControlList().getGrants().size() == 0) {
            //Add default 'fullcontrol' grant for owner.
            acPolicy.getAccessControlList().getGrants().add(new Grant(new Grantee(buildCanonicalUser(requestUser.getAccount())), ObjectStorageProperties.Permission.FULL_CONTROL.toString()));
        }

        if (bucketOwnerCanonicalId != null) {
            acPolicy.setAccessControlList(
                    AclUtils.expandCannedAcl(acPolicy.getAccessControlList(), bucketOwnerCanonicalId, requestUser.getAccount().getCanonicalId()));
        } else {
            acPolicy.setAccessControlList(
                    AclUtils.expandCannedAcl(acPolicy.getAccessControlList(), requestUser.getAccount().getCanonicalId(), null));
        }

        return acPolicy;
    }
}
