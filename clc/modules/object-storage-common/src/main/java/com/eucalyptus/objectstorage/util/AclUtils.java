/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidArgumentException;
import com.eucalyptus.objectstorage.exceptions.s3.UnresolvableGrantByEmailAddressException;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Maps CannedAcls to access control lists
 */
public class AclUtils {
  private static final Logger LOG = Logger.getLogger(AclUtils.class);

  /* Use string as key since enum doesn't allow '-' which is in the ACL types. Allows a direct lookup from the msg */
  private static final HashMap<String, Function<OwnerIdPair, List<Grant>>> cannedAclMap = new HashMap<String, Function<OwnerIdPair, List<Grant>>>();

  // A lookup map for quick verification of group uris
  private static final HashMap<String, ObjectStorageProperties.S3_GROUP> groupUriMap = new HashMap<String, ObjectStorageProperties.S3_GROUP>();

  static {
    // Populate the map
    cannedAclMap.put(ObjectStorageProperties.CannedACL.private_only.toString(), PrivateOnlyGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.authenticated_read.toString(), AuthenticatedReadGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.public_read.toString(), PublicReadGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.public_read_write.toString(), PublicReadWriteGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.aws_exec_read.toString(), AwsExecReadGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.ec2_bundle_read.toString(), Ec2BundleReadGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_full_control.toString(), BucketOwnerFullControlGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.bucket_owner_read.toString(), BucketOwnerReadGrantBuilder.INSTANCE);
    cannedAclMap.put(ObjectStorageProperties.CannedACL.log_delivery_write.toString(), LogDeliveryWriteGrantBuilder.INSTANCE);

    for (ObjectStorageProperties.S3_GROUP g : ObjectStorageProperties.S3_GROUP.values()) {
      groupUriMap.put(g.toString(), g);
    }
  }

  public static boolean isValidCannedAcl(String candidateAcl) {
    return cannedAclMap.containsKey(candidateAcl);
  }

  /*
   * Simply determines if the userId is a member of the groupId, very simplistic only for ALL_USERS and AUTHENTICATED_USERS, not arbitrary groups.
   * Arbitrary groups are not yet supported in ObjectStorage bucket policies/IAM policies. userId should be a canonicalId
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
   * Just checks the basic S3 groups for membership of the userId. Caller must ensure that the userId is a valid ID in the system. That is outside the
   * scope of this method.
   *
   * @param userId The s3 user id, i.e. the accounts canonical identifier
   * @param group The group to check membership of
   * @return true if a member
   */
  public static boolean isUserMember(String userId, ObjectStorageProperties.S3_GROUP group) {
    if ( group == null || Strings.isNullOrEmpty( userId ) ) {
      return false;
    }

    if (ObjectStorageProperties.S3_GROUP.ALL_USERS_GROUP.equals(group)) {
      return true;
    }

    if (ObjectStorageProperties.S3_GROUP.AUTHENTICATED_USERS_GROUP.equals(group) &&
        !Principals.nobodyUser( ).getCanonicalId( ).equals( userId ) ) {
      return true;
    }

    if (ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ.equals(group) ||
        ObjectStorageProperties.S3_GROUP.AWS_EXEC_READ.equals(group)) {
      try {
        return Accounts.lookupSystemAccountByAlias( AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT ).getCanonicalId( )
            .equals( userId );
      } catch (AuthException e) {
        // Fall through
        LOG.debug("Got auth exception trying to lookup aws-exec-read admin user for group membership check in ec2-bundle-read", e);
      }
    }

    // System or euca/admin only in logging
    if (ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.equals(group)) {
      try {
        return
            Principals.systemUser( ).getCanonicalId( ).equals(userId) ||
            Accounts.lookupSystemAdmin( ).getCanonicalId( ).equals(userId);
      } catch (AuthException e) {
        // Fall through
        LOG.debug("Got auth exception trying to lookup system admin user for group membership check in ec2-bundle-read", e);
      }
    }

    return false;
  }

  /**
   * Utility class for passing pairs of canonicalIds around without using something ambiguous like an String-array.
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
   * If the object ownerId is set in the OwnerIdPair then this will assume that the resource is an object and will return a full-control grant for
   * that user. If bucket owner only is set then this assumes that the bucket is the resource.
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
        displayName = lookupDisplayNameByCanonicalId(ownerCanonicalId);
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

  /**
   * This is inconsistent with S3, because we use a group rather than account for the grant. Makes more sense for euca and can be changed later if
   * needed via upgrade
   */
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

  /**
   * This is inconsistent with S3, because we use a group rather than account for the grant. Makes more sense for euca and can be changed later if
   * needed via upgrade
   */
  protected enum Ec2BundleReadGrantBuilder implements Function<OwnerIdPair, List<Grant>> {
    INSTANCE;

    @Override
    public List<Grant> apply(OwnerIdPair ownerIds) {
      List<Grant> grants = PrivateOnlyGrantBuilder.INSTANCE.apply(ownerIds);
      Grantee grantee = new Grantee();
      grantee.setGroup(new Group(ObjectStorageProperties.S3_GROUP.EC2_BUNDLE_READ.toString()));
      Grant grant = new Grant();
      grant.setPermission(ObjectStorageProperties.Permission.READ.toString());
      grant.setGrantee(grantee);
      grants.add(grant);
      return grants;
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
        displayName = lookupDisplayNameByCanonicalId(canonicalId);
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
        displayName = lookupDisplayNameByCanonicalId(canonicalId);
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
   * Processes a list by finding all canned-acls and expanding those. The returned list is a new list that includes all non-canned ACL entries of the
   * input as well as the expanded grants mapped to canned-acls
   * <p/>
   * CannedAcls are Grants with Grantee = "", and Permision is the canned-acl string
   *
   * @param msgAcl
   * @return
   */
  public static AccessControlList expandCannedAcl(@Nonnull AccessControlList msgAcl, @Nullable final String bucketOwnerCanonicalId,
      @Nullable final String objectOwnerCanonicalId) throws EucalyptusCloudException {
    if (msgAcl == null) {
      throw new IllegalArgumentException("Null list received");
    }

    AccessControlList outputList = new AccessControlList();
    if (outputList.getGrants() == null) {
      // Should be handled by constructor of ACL, but just to be sure
      outputList.setGrants(new ArrayList<Grant>());
    }
    final OwnerIdPair owners = new OwnerIdPair(bucketOwnerCanonicalId, objectOwnerCanonicalId);
    String entryValue = null;
    for (Grant msgGrant : msgAcl.getGrants()) {
      entryValue = msgGrant.getPermission(); // The OSG binding populates the canned-acl in the permission field
      try {
        if (cannedAclMap.containsKey(entryValue)) {
          outputList.getGrants().addAll(cannedAclMap.get(entryValue).apply(owners));
        } else {
          // add to output.
          outputList.getGrants().add(msgGrant);
        }
      } catch (Exception e) {
        // Failed. Stop now
        throw new EucalyptusCloudException("Failed generating the full ACL from canned ACL", e);
      }
    }
    return outputList;
  }

  public static CanonicalUser buildCanonicalUser( UserPrincipal user ) {
    return new CanonicalUser( user.getCanonicalId( ), user.getAccountAlias( ) );
  }

  /**
   * Ensures the the policy is not empty. If found empty or null, a 'private' policy is generated and returned. If creating for an object, the
   * BucketOwnerCanonicalId must not be null. If found null, then a bucket-creation is expected and ACLs will be expanded as such.
   *
   * @param requestUser
   * @param policy
   * @return
   */
  public static AccessControlPolicy processNewResourcePolicy(@Nonnull UserPrincipal requestUser, @Nullable AccessControlPolicy policy,
      @Nullable String bucketOwnerCanonicalId) throws Exception {
    AccessControlPolicy acPolicy = null;
    if (policy != null) {
      acPolicy = policy;
    } else {
      acPolicy = new AccessControlPolicy();
    }

    if (acPolicy.getOwner() == null) {
      acPolicy.setOwner(buildCanonicalUser(requestUser));
    }

    if (acPolicy.getAccessControlList() == null) {
      acPolicy.setAccessControlList(new AccessControlList());
    }

    if (acPolicy.getAccessControlList().getGrants() == null || acPolicy.getAccessControlList().getGrants().size() == 0) {
      // Add default 'fullcontrol' grant for owner.
      acPolicy.getAccessControlList().getGrants()
          .add(new Grant(new Grantee(buildCanonicalUser(requestUser)), ObjectStorageProperties.Permission.FULL_CONTROL.toString()));
    }

    final String canonicalId = requestUser.getCanonicalId( );
    if (bucketOwnerCanonicalId != null) {
      acPolicy.setAccessControlList(AclUtils.expandCannedAcl(acPolicy.getAccessControlList(), bucketOwnerCanonicalId, canonicalId));
    } else {
      acPolicy.setAccessControlList(AclUtils.expandCannedAcl(acPolicy.getAccessControlList(), canonicalId, null));
    }

    return acPolicy;
  }

  /**
   * Checks grants and transforms grantees into canonicalId from eucalyptus account id or email address
   *
   * @param acl
   * @return
   */
  public static AccessControlList scrubAcl(AccessControlList acl) {
    AccessControlList scrubbed = new AccessControlList();
    if (acl == null || acl.getGrants() == null || acl.getGrants().size() == 0) {
      return scrubbed;
    }

    String canonicalId = null;
    Grantee grantee;
    CanonicalUser canonicalUser;
    Group group;
    String email;

    for (Grant g : acl.getGrants()) {
      grantee = g.getGrantee();
      if (grantee == null) {
        continue; // skip, no grantee
      } else {
        canonicalUser = grantee.getCanonicalUser();
        group = grantee.getGroup();
        email = grantee.getEmailAddress();
      }

      canonicalId = canonicalUser == null ? null : resolveCanonicalId(canonicalUser.getID());
      if (canonicalId == null) {
        try {
          canonicalId = Accounts.lookupCanonicalIdByEmail( email );
        } catch (AuthException authEx) {
          // no-op, we'll check the group
        }
      }
      if (canonicalId == null && group != null && !Strings.isNullOrEmpty(group.getUri())) {
        ObjectStorageProperties.S3_GROUP foundGroup = AclUtils.getGroupFromUri(group.getUri());
        if (foundGroup == null) {
          throw new NoSuchElementException("URI: " + group.getUri() + " not found in group map");
        }
        // Group URI, use as canonicalId for now.
        canonicalId = group.getUri();
      }

      // the following exception handling looks strange, but this method gets called inside of an enum that
      // implements Function<> (guava) and since the targets are checked exceptions it is not easy to pass them
      // all the way up the stack where they become significant.
      if (email != null && !"".equals(email) && canonicalId == null) {
        // build up and throw InvalidArgumentException for the email
        UnresolvableGrantByEmailAddressException ugbeae = new UnresolvableGrantByEmailAddressException();
        ugbeae.setEmailAddress(email);
        throw new RuntimeException(ugbeae);
      } else if (canonicalId == null) {
        // throw new NoSuchElementException("No canonicalId found for grant: " + g.toString());
        InvalidArgumentException iae = new InvalidArgumentException();
        iae.setMessage("Invalid id");
        iae.setArgumentName("CanonicalUser/ID");
        iae.setArgumentValue(canonicalUser.getID());
        throw new RuntimeException(iae);
      } else {
        if (grantee.getCanonicalUser() == null) {
          grantee.setCanonicalUser(new CanonicalUser(canonicalId, ""));
        } else {
          grantee.getCanonicalUser().setID(canonicalId);
        }
      }
    }

    return acl;
  }

  private static interface CanonicalIdChecker {
    String check(String id);
  }

  private static List<CanonicalIdChecker> canonicalIdCheckers = Lists.newArrayList(

  new CanonicalIdChecker() {
    // is it a canonicalId?
    @Override
    public String check(String id) {
      try {
        UserPrincipal userPrincipal = lookupPrincipalByCanonicalId( id );
        return userPrincipal.getCanonicalId();
      } catch (AuthException authEx) {
      }
      return null;
    }
  },

  new CanonicalIdChecker() {
    // is it a eucalyptus account id?
    @Override
    public String check(String id) {
      if ( Accounts.isAccountNumber( id ) ) try {
        UserPrincipal userPrincipal = Accounts.lookupPrincipalByAccountNumber( id );
        return userPrincipal.getCanonicalId();
      } catch (AuthException authEx) {
      }
      return null;
    }
  },

  new CanonicalIdChecker() {
    // is it an email address?
    @Override
    public String check(String id) {
      try {
        if ( Strings.nullToEmpty( id ).contains( "@" ) ) {
          return Accounts.lookupCanonicalIdByEmail( id );
        }
        return null;
      } catch (AuthException authEx) {
        return null;
      }
    }
  });

  public static String resolveCanonicalId(final String inputCanonicalId) {
    String resultCanonicalId = null;
    for (CanonicalIdChecker checker : canonicalIdCheckers) {
      resultCanonicalId = checker.check(inputCanonicalId);
      if (resultCanonicalId != null) {
        return resultCanonicalId;
      }
    }
    return null;
  }

  public static String lookupDisplayNameByCanonicalId( final String canonicalId ) throws AuthException {
    if ( AccountIdentifiers.NOBODY_CANONICAL_ID.equals( canonicalId ) ) {
      return Principals.nobodyAccount( ).getAccountAlias( );
    } else {
      return Accounts.lookupAccountIdentifiersByCanonicalId( canonicalId ).getAccountAlias();
    }
  }

  public static UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    if ( AccountIdentifiers.NOBODY_CANONICAL_ID.equals( canonicalId ) ) {
      LOG.info("Looked up nobody by canonical ID " + canonicalId);
      return Principals.nobodyUser();
    } else {
      return Accounts.lookupPrincipalByCanonicalId( canonicalId );
    }
  }

}
