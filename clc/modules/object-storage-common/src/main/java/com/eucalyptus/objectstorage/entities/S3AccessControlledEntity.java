/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.AbstractStatefulStacklessPersistent;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.google.common.base.Function;

/**
 * Common handler for authorization for S3 resources that have access controls via ACLs
 *
 * @author zhill
 */
@MappedSuperclass
public abstract class S3AccessControlledEntity<STATE extends Enum<STATE>> extends AbstractStatefulStacklessPersistent<STATE> {
  @Transient
  private static final Logger LOG = Logger.getLogger(S3AccessControlledEntity.class);

  // Display name for IAM user
  @Column(name = "owner_iam_user_displayname")
  protected String ownerIamUserDisplayName;

  // Hold the real owner ID. This is the canonical user Id.
  @Column(name = "owner_canonical_id", nullable = false, length = 64)
  protected String ownerCanonicalId;

  // Needed for enforcing IAM resource quotas (an extension of IAM for Euca)
  @Column(name = "owner_iam_user_id", nullable = false, length = 64)
  protected String ownerIamUserId;

  // 8k size should cover 100 80-byte entries which allows for json chars etc
  @Column(name = "acl", nullable = false)
  @Type( type = "text" )
  private String acl; // A JSON encoded string that is the acl list.

  @Column(name = "owner_displayname")
  protected String ownerDisplayName;

  /**
   * Map for running actual checks against. Saved to optimize multiple accesses. Caching
   */
  @Transient
  private Map<String, Integer> decodedAcl = null;

  // Lob types don't like comparisons with null, so ensure that doesn't happen.
  @PostLoad
  @PrePersist
  public void nullChecks() {
    if (acl == null) {
      acl = "{}"; // empty json
    }
  }

  /**
   * Returns the full name of the resource. e.g. bucket/object for objects or bucket for bucket
   *
   * @return
   */
  public abstract String getResourceFullName();

  public String getOwnerCanonicalId() {
    return ownerCanonicalId;
  }

  public void setOwnerCanonicalId(String ownerCanonicalId) {
    this.ownerCanonicalId = ownerCanonicalId;
  }

  public String getOwnerIamUserId() {
    return ownerIamUserId;
  }

  public void setOwnerIamUserId(String ownerIamUserId) {
    this.ownerIamUserId = ownerIamUserId;
  }

  public String getOwnerDisplayName() {
    return ownerDisplayName;
  }

  public void setOwnerDisplayName(String ownerDisplayName) {
    this.ownerDisplayName = ownerDisplayName;
  }

  /**
   * For internal and JPA use only. This returns a json string
   *
   * @return
   */
  protected String getAcl() {
    return this.acl;
  }

  public void setAcl(String aclString) {
    this.acl = aclString;
    setDecodedAcl(null);
  }

  /**
   * Set from the messaging type. The owner must be properly set in the message msgAcl.
   *
   * @param msgAcl
   */
  public void setAcl(final AccessControlPolicy msgAcl) {
    AccessControlPolicy policy = msgAcl;

    // Check for the owner and add it if not already set
    if (this.getOwnerCanonicalId() != null) {
      if (policy.getOwner() != null && !StringUtils.equals(policy.getOwner().getID(), this.getOwnerCanonicalId())) {
        throw new RuntimeException("Owner cannot be changed");
      } else if (policy.getOwner() == null) {
        // Copy into policy from this object
        policy.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
      }
    }

    Map<String, Integer> resultMap = AccessControlPolicyToMap.INSTANCE.apply(policy);

    // Serialize into json
    setAcl(JSONObject.fromObject(resultMap).toString());
    setDecodedAcl(resultMap);
  }

  /**
   * Utility method for getting the string version of an access control list
   *
   * @param acl
   * @return
   */
  public static String marshallAclToString(AccessControlList acl) throws Exception {
    Map<String, Integer> resultMap = AccessControlListToMap.INSTANCE.apply(acl);

    // Serialize into json
    return JSONObject.fromObject(resultMap).toString();
  }

  public static String marshallAcpToString(AccessControlPolicy acl) throws Exception {
    Map<String, Integer> resultMap = AccessControlPolicyToMap.INSTANCE.apply(acl);

    // Serialize into json
    return JSONObject.fromObject(resultMap).toString();
  }

  /**
   * Returns the message-typed version of the acl policy. Not necessary for evaluation, just presentation to the user
   *
   * @return
   * @throws Exception
   */
  public AccessControlPolicy getAccessControlPolicy() throws Exception {
    AccessControlPolicy policy = MapToAccessControlPolicy.INSTANCE.apply(getDecodedAcl());
    policy.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.ownerDisplayName));
    return policy;
  }

  public String getOwnerIamUserDisplayName() {
    return ownerIamUserDisplayName;
  }

  public void setOwnerIamUserDisplayName(String ownerIamUserDisplayName) {
    this.ownerIamUserDisplayName = ownerIamUserDisplayName;
  }

  /**
   * Authorization check for requested permission by specified user/account via canonicalId Currently only checks the ACLs.
   *
   * @param permission
   * @return
   */
  public boolean can(ObjectStorageProperties.Permission permission, String canonicalId) {
    try {
      Map<String, Integer> myAcl = getDecodedAcl();
      if (myAcl == null) {
        LOG.error("Got null acl, cannot authorize " + permission.toString());
        return false;
      }

      /*
       * Grants can only allow access, so return true if *any* gives the user access.
       */
      // Check groups first.
      String groupName;
      for (ObjectStorageProperties.S3_GROUP group : ObjectStorageProperties.S3_GROUP.values()) {
        if (can(permission, group) && AclUtils.isUserMemberOfGroup(canonicalId, group.toString())) {
          // User is member of group and the group has permission
          return true;
        }
      }

      if (myAcl.containsKey(canonicalId) && BitmapGrant.allows(permission, myAcl.get(canonicalId))) {
        // Explicitly granted by canonical Id.
        return true;
      } else {
        // fall through
      }
    } catch (Exception e) {
      LOG.error("Error checking authorization", e);
    }
    return false;
  }

  public boolean can(ObjectStorageProperties.Permission permission, ObjectStorageProperties.S3_GROUP group) {
    try {
      Map<String, Integer> myAcl = getDecodedAcl();
      if (myAcl == null) {
        LOG.error("Got null acl, cannot authorize " + permission.toString());
        return false;
      }

      String groupName = group.toString();
      return (myAcl.containsKey(groupName) && BitmapGrant.allows(permission, myAcl.get(groupName)));
    } catch (Exception e) {
      LOG.error("Error checking authorization", e);
    }
    return false;
  }

  private synchronized void setDecodedAcl(Map<String, Integer> permissionsMap) {
    this.decodedAcl = permissionsMap;
  }

  private synchronized Map<String, Integer> getDecodedAcl() throws Exception {
    if (this.decodedAcl == null) {
      try {
        // Jackson requires this method to handle generics
        Map<String, Integer> aclMap = new HashMap<String, Integer>();
        JSONObject aclJson = (JSONObject) JSONSerializer.toJSON(this.getAcl());
        String key = null;
        Iterator keys = aclJson.keys();
        while (keys.hasNext()) {
          key = (String) keys.next();
          aclMap.put((String) key, new Integer(aclJson.getInt((String) key)));
        }
        setDecodedAcl(aclMap);
      } catch (Exception e) {
        setDecodedAcl(null);
        LOG.error("Error decoding acl from DB string", e);
        throw e;
      }
    }
    return this.decodedAcl;
  }

  /**
   * Converts internal representation into the messaging representation. NOTE: does NOT add owner information as the owner is unknown at this level.
   * The caller must find the owner grant and set it specifically.
   *
   * @author zhill
   */
  protected enum MapToAccessControlPolicy implements Function<Map<String, Integer>, AccessControlPolicy> {
    INSTANCE;

    @Override
    public AccessControlPolicy apply(Map<String, Integer> srcMap) {
      AccessControlPolicy policy = new AccessControlPolicy();
      AccessControlList acList = new AccessControlList();
      ArrayList<Grant> grants = new ArrayList<Grant>();
      String displayName = null;
      for (Map.Entry<String, Integer> entry : srcMap.entrySet()) {
        Grantee grantee = new Grantee();

        // Check if a group uri
        ObjectStorageProperties.S3_GROUP groupId = null;
        try {
          groupId = AclUtils.getGroupFromUri(entry.getKey());
        } catch (Exception e) {
        }
        if (groupId != null) {
          grantee.setGroup(new Group(groupId.toString()));
          grantee.setType("Group");
        } else {
          try {
            displayName = AclUtils.lookupDisplayNameByCanonicalId(entry.getKey());
            } catch (AuthException e) {
            // Not found
            displayName = "";
          }
          grantee.setCanonicalUser(new CanonicalUser(entry.getKey(), displayName));
          grantee.setType("CanonicalUser");
        }

        for (ObjectStorageProperties.Permission p : AccountGrantsFromBitmap.INSTANCE.apply(entry.getValue())) {
          grants.add(new Grant(grantee, p.toString()));
        }
      }
      acList.setGrants(grants);
      policy.setAccessControlList(acList);
      return policy;
    }
  }

  /**
   * Convert the specified AccessControlPolicy type (a messaging type) into the persistence type
   */
  protected enum AccessControlPolicyToMap implements Function<AccessControlPolicy, Map<String, Integer>> {
    INSTANCE;

    /**
     * Returns a valid map of canonicalid -> grant.
     *
     * @return The canonicalid -> grant map
     * @throws RuntimeException if an error occurred.
     */
    @Nonnull
    @Override
    public Map<String, Integer> apply( final AccessControlPolicy srcPolicy ) {
      if (srcPolicy == null) {
        throw new RuntimeException("Null source policy. Cannot map");
      }

      final Map<String, Integer> aclMap = AccessControlListToMap.INSTANCE.apply( srcPolicy.getAccessControlList( ) );
      if ( aclMap == null ) {
        throw new RuntimeException("Null acl map. Cannot proceed with policy generation");
      }

      // Check for valid owner
      final String ownerCanonicalId = srcPolicy.getOwner().getID();
      if ( ownerCanonicalId == null ) {
        throw new RuntimeException("Invalid ACP: OwnerCanonicalId required.");
      } else {
        try {
          AclUtils.lookupPrincipalByCanonicalId(ownerCanonicalId);
        } catch ( Exception e ) {
          LOG.warn("Got invalid owner in AccessControlPolicy during mapping to DB: " + ownerCanonicalId);
          throw new RuntimeException("Could not find account by canonicalId " + ownerCanonicalId, e);
        }
      }

      return aclMap;
    }
  }

  /**
   * Handles just the access control list itself without the additional owner info
   *
   * @author zhill
   */
  protected enum AccessControlListToMap implements Function<AccessControlList, Map<String, Integer>> {
    INSTANCE;

    /**
     * Returns a valid map of canonicalid -> grant. Returns null if an error occurred.
     */
    @Override
    public Map<String, Integer> apply(AccessControlList srcList) {
      HashMap<String, Integer> aclMap = new HashMap<String, Integer>();

      if (srcList == null) {
        // Nothing to do
        return aclMap;
      }

      AclUtils.scrubAcl(srcList);

      for (Grant g : srcList.getGrants()) {
        String canonicalId = g.getGrantee().getCanonicalUser().getID();
        int oldGrant = (aclMap.containsKey(canonicalId) ? aclMap.get(canonicalId) : 0);
        int newGrant = BitmapGrant.add(ObjectStorageProperties.Permission.valueOf(g.getPermission().toUpperCase()), oldGrant);
        if (newGrant != 0) {
          aclMap.put(canonicalId, newGrant);
        } else {
          // skip no-op grants
        }
      }

      return aclMap;
    }
  }

  /**
   * Converts from BitmapGrant to 1 or more msg Grants (up to 3). Does not set the grantee on the grants, just the permission(s).
   * <p/>
   * Represents the grant(s) for a single canonicalId/group
   */
  protected enum AccountGrantsFromBitmap implements Function<Integer, List<ObjectStorageProperties.Permission>> {
    INSTANCE;

    @Override
    public List<ObjectStorageProperties.Permission> apply(Integer srcBitmap) {
      ArrayList<ObjectStorageProperties.Permission> permissions = new ArrayList<ObjectStorageProperties.Permission>();
      if (srcBitmap == null) {
        return permissions;
      }

      if (BitmapGrant.allows(ObjectStorageProperties.Permission.FULL_CONTROL, srcBitmap)) {
        permissions.add(ObjectStorageProperties.Permission.FULL_CONTROL);
      } else {

        int i = 0;
        if (BitmapGrant.allows(ObjectStorageProperties.Permission.READ, srcBitmap)) {
          permissions.add(ObjectStorageProperties.Permission.READ);
        }

        if (BitmapGrant.allows(ObjectStorageProperties.Permission.WRITE, srcBitmap)) {
          permissions.add(ObjectStorageProperties.Permission.WRITE);
        }

        if (BitmapGrant.allows(ObjectStorageProperties.Permission.READ_ACP, srcBitmap)) {
          permissions.add(ObjectStorageProperties.Permission.READ_ACP);
        }

        if (BitmapGrant.allows(ObjectStorageProperties.Permission.WRITE_ACP, srcBitmap)) {
          permissions.add(ObjectStorageProperties.Permission.WRITE_ACP);
        }
      }
      return permissions;
    }
  }

  /**
   * Implements the mapping of perms to the bitmap Mapping: Integer => read,write,readAcp,writeAcp in least significant bits Example: read = 8, write
   * = 4, ...
   *
   * @author zhill
   */
  public enum BitmapGrant {
    INSTANCE;

    private static final int readMask = 8; // 4th bit from right
    private static final int writeMask = 4; // 3rd bit
    private static final int readACPMask = 2; // 2nd bit
    private static final int writeACPMask = 1; // 1st bit

    /**
     * Does this grant allow the requested permission?
     *
     * @param perm
     * @return
     */
    public static boolean allows(ObjectStorageProperties.Permission perm, int mapValue) {
      switch (perm) {
        case FULL_CONTROL:
          return (mapValue & (readMask | writeMask | readACPMask | writeACPMask)) == (readMask | writeMask | readACPMask | writeACPMask);
        case READ:
          return (mapValue & readMask) == readMask;
        case WRITE:
          return (mapValue & writeMask) == writeMask;
        case READ_ACP:
          return (mapValue & readACPMask) == readACPMask;
        case WRITE_ACP:
          return (mapValue & writeACPMask) == writeACPMask;
      }
      return false;
    }

    /**
     * Sets to the requested permission ONLY. Not additive, replaces previous value.
     *
     * @param perm
     */
    public static int translateToBitmap(ObjectStorageProperties.Permission perm) {
      switch (perm) {
        case FULL_CONTROL:
          return (readMask | writeMask | readACPMask | writeACPMask);
        case READ:
          return readMask;
        case WRITE:
          return writeMask;
        case READ_ACP:
          return readACPMask;
        case WRITE_ACP:
          return writeACPMask;
      }
      return 0;
    }

    /**
     * Add the specified permission, non-destructive. Returns a new bitmap that adds the requested permission to the oldAclBitmap
     *
     * @param perm
     */
    public static int add(ObjectStorageProperties.Permission perm, int oldAclBitmap) {
      switch (perm) {
        case FULL_CONTROL:
          return (int) (oldAclBitmap | readMask | writeMask | readACPMask | writeACPMask);
        case READ:
          return (int) (oldAclBitmap | readMask);
        case WRITE:
          return (int) (oldAclBitmap | writeMask);
        case READ_ACP:
          return (int) (oldAclBitmap | readACPMask);
        case WRITE_ACP:
          return (int) (oldAclBitmap | writeACPMask);
      }
      return 0;
    }

    /**
     * Returns a log-friendly string of the map in b
     *
     * @param b
     * @return
     */
    public static String toLogString(Integer b) {
      StringBuilder sb = new StringBuilder("{");
      sb.append("read=").append(allows(ObjectStorageProperties.Permission.READ, b));
      sb.append(",write=").append(allows(ObjectStorageProperties.Permission.WRITE, b));
      sb.append(",readacp=").append(allows(ObjectStorageProperties.Permission.READ_ACP, b));
      sb.append(",writeacp=").append(allows(ObjectStorageProperties.Permission.WRITE_ACP, b));
      sb.append("}");
      return sb.toString();
    }
  }
}
