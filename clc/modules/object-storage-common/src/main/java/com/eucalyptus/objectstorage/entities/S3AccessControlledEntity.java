/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.google.common.base.Function;
import com.google.common.base.Strings;

/**
 * Common handler for authorization for S3 resources that have access controls via ACLs
 * 
 * @author zhill
 *
 */
@MappedSuperclass
public abstract class S3AccessControlledEntity extends AbstractPersistent {
	@Transient
	private static final Logger LOG = Logger.getLogger(S3AccessControlledEntity.class);
	
	// Hold the real owner ID. This is the canonical user Id.
	@Column( name = "owner_canonical_id" )
	protected String ownerCanonicalId;
	
	//Needed for enforcing IAM resource quotas (an extension of IAM for Euca)
	@Column( name = "owner_iam_user_id" )
	protected String ownerIamUserId;

	//8k size should cover 100 80-byte entries which allows for json chars etc 
	@Column( name = "acl", length=8192)
	private String acl; //A JSON encoded string that is the acl list.
	
	@Column( name = "owner_displayname")
	protected String ownerDisplayName;
	
	/**
	 * Map for running actual checks against. Saved to optimize multiple accesses. Caching
	 */
	@Transient
	private Map<String, Integer> decodedAcl = null;
	
	//Lob types don't like comparisons with null, so ensure that doesn't happen.
	@PostLoad
	@PrePersist
	public void nullChecks() {
		if(acl == null) {
			acl = "{}"; //empty json
		}
	}
	
	/**
	 * Returns the full name of the resource. e.g. bucket/object for objects or bucket for bucket
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
	 * Set from the messaging type. The owner must be properly set in
	 * the message msgAcl.
	 * @param msgAcl
	 */
	public void setAcl(final AccessControlPolicy msgAcl) throws Exception {
		AccessControlPolicy policy = msgAcl;
		
		//Add the owner info if not already set
		if(policy.getOwner() == null && this.getOwnerCanonicalId() != null) {
			policy.setOwner(new CanonicalUser(this.getOwnerCanonicalId(),this.getOwnerDisplayName()));
		} else {
			//Already present or can't be set
		}
		
		Map<String, Integer> resultMap = AccessControlPolicyToMap.INSTANCE.apply(policy);
		
		//Serialize into json
		setAcl(JSONObject.fromObject(resultMap).toString());
		setDecodedAcl(resultMap);		
	}

	/**
	 * Utility method for getting the string version of an access control list
	 * @param acl
	 * @return
	 */
	public static String marshallAclToString(AccessControlList acl) {
		Map<String, Integer> resultMap = AccessControlListToMap.INSTANCE.apply(acl);
				
		//Serialize into json
		return JSONObject.fromObject(resultMap).toString();
	}
	
	public static String marshallACPToString(AccessControlPolicy acl) {
		Map<String, Integer> resultMap = AccessControlPolicyToMap.INSTANCE.apply(acl);
				
		//Serialize into json
		return JSONObject.fromObject(resultMap).toString();
	}
	
	/**
	 * Returns the message-typed version of the acl policy.
	 * Not necessary for evaluation, just presentation to the user
	 * @return
	 * @throws Exception
	 */
	public AccessControlPolicy getAccessControlPolicy() throws Exception {
		AccessControlPolicy policy = MapToAccessControlPolicy.INSTANCE.apply(getDecodedAcl());
		policy.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.ownerDisplayName));
		return policy;
	}
	
	/**
	 * Authorization check for requested permission by specified user/account via canonicalId
	 * Currently only checks the ACLs.
	 * @param permission
	 * @return
	 */
	public boolean can(ObjectStorageProperties.Permission permission, String canonicalId) {
		try {
			Map<String, Integer> myAcl = getDecodedAcl();
			if(myAcl == null) {
				LOG.error("Got null acl, cannot authorize " + permission.toString());
				return false;
			}
			
			/*
			 * Grants can only allow access, so return true if *any* gives the user access.
			 */
			//Check groups first.
			String groupName;
			for(ObjectStorageProperties.S3_GROUP group : ObjectStorageProperties.S3_GROUP.values()) {
				if(can(permission, group) && AclUtils.isUserMemberOfGroup(canonicalId, group.toString())) {
					//User is member of group and the group has permission
					return true;
				}
			}
			
			if(myAcl.containsKey(canonicalId) && BitmapGrant.allows(permission,myAcl.get(canonicalId))) {
				//Explicitly granted by canonical Id.
				return true;
			} else {
				//fall through
			}
		} catch(Exception e) {
			LOG.error("Error checking authorization",e);
		}
		return false;
	}
	
	public boolean can(ObjectStorageProperties.Permission permission, ObjectStorageProperties.S3_GROUP group) {
		try {
			Map<String, Integer> myAcl = getDecodedAcl();
			if(myAcl == null) {
				LOG.error("Got null acl, cannot authorize " + permission.toString());
				return false;
			}
			
			String groupName = group.toString();
			return (myAcl.containsKey(groupName) && BitmapGrant.allows(permission, myAcl.get(groupName)));
		} catch(Exception e) {
			LOG.error("Error checking authorization",e);
		}
		return false;
	}
	
	private synchronized void setDecodedAcl(Map<String, Integer> permissionsMap) {
		this.decodedAcl = permissionsMap;
	}
	
	private synchronized Map<String, Integer> getDecodedAcl() throws Exception {
		if(this.decodedAcl == null) {
			try {
				//Jackson requires this method to handle generics
				Map<String, Integer> aclMap = new HashMap<String,Integer>();
				JSONObject aclJson = (JSONObject)JSONSerializer.toJSON(this.getAcl());
				String key = null;
				Iterator keys = aclJson.keys();
				while(keys.hasNext()) {
					key = (String)keys.next();
					aclMap.put((String)key, new Integer(aclJson.getInt((String)key)));
				}
				setDecodedAcl(aclMap);
			} catch(Exception e) {
				setDecodedAcl(null);
				LOG.error("Error decoding acl from DB string",e);	
				throw e;
			}
		}	
		return this.decodedAcl;
	}
	
	/**
	 * Converts internal representation into the messaging representation.
	 * NOTE: does NOT add owner information as the owner is unknown at this level.
	 * The caller must find the owner grant and set it specifically.
	 * @author zhill
	 *
	 */
	protected enum MapToAccessControlPolicy implements Function<Map<String, Integer>, AccessControlPolicy> {
		INSTANCE;
		
		@Override
		public AccessControlPolicy apply(Map<String, Integer> srcMap) {
			AccessControlPolicy policy = new AccessControlPolicy();
			AccessControlList acList = new AccessControlList();
			ArrayList<Grant> grants = new ArrayList<Grant>();
			String displayName = null;
			for(Map.Entry<String,Integer> entry : srcMap.entrySet()) {
				Grantee grantee = new Grantee();				

				//Check if a group uri
				ObjectStorageProperties.S3_GROUP groupId = null;
				try {
					groupId = AclUtils.getGroupFromUri(entry.getKey());					
				} catch(Exception e) {}				
				if(groupId != null) {
					grantee.setGroup(new Group(groupId.toString()));
					grantee.setType("Group");
				} else {
					try {
						displayName = Accounts.lookupAccountByCanonicalId(entry.getKey()).getName();
					} catch(AuthException e) {
						//Not found
						displayName = "";
					}
					grantee.setCanonicalUser(new CanonicalUser(entry.getKey(),displayName));
					grantee.setType("CanonicalUser");
				}
				
				for(ObjectStorageProperties.Permission p : AccountGrantsFromBitmap.INSTANCE.apply(entry.getValue())) {
					grants.add(new Grant(grantee, p.toString()));
				}
			}
			acList.setGrants(grants);
			policy.setAccessControlList(acList);
			return policy;
		}
	}

	/**
	 * Convert the specified AccessControlPolicy type (a messaging type) into
	 * the persistence type
	 * @param msg
	 */	
	protected enum AccessControlPolicyToMap implements Function<AccessControlPolicy, Map<String, Integer>> {
		INSTANCE;	
		
		/**
		 * Returns a valid map of canonicalid -> grant. Returns null if an error occurred.
		 */
		@Override
		public Map<String, Integer> apply(AccessControlPolicy srcPolicy) {
			Map<String, Integer> aclMap = null;
			aclMap = AccessControlListToMap.INSTANCE.apply(srcPolicy.getAccessControlList());
			
			//Should be an empty map if nothing in the list. null is an error.
			if(aclMap == null) {
				LOG.warn("Got null from AccessControlList mapping, indicates an error");
				return null;
			} else {
				//Add owner
				String ownerCanonicalId = srcPolicy.getOwner().getID();
				if(ownerCanonicalId == null) {
					//Owner is required.
					return null;
				}
				
				//Check for valid owner
				try {
					Accounts.lookupAccountByCanonicalId(ownerCanonicalId);
				} catch(Exception e) {
					//Invalid owner
					LOG.warn("Got invalid owner in AccessControlPolicy during mapping to DB: " + ownerCanonicalId);
					return null;
				}
								
				//Owner always has full control
				aclMap.remove(ownerCanonicalId); //remove any previous entry, only one, FULL_CONTROL even if policy was redundant
				Integer ownerGrant = BitmapGrant.translateToBitmap(ObjectStorageProperties.Permission.FULL_CONTROL);
				aclMap.put(ownerCanonicalId, ownerGrant);
			}
			return aclMap;
		}
	}
	
	/**
	 * Handles just the access control list itself without the additional owner info
	 * @author zhill
	 *
	 */
	protected enum AccessControlListToMap implements Function<AccessControlList, Map<String, Integer>> {
		INSTANCE;	
		
		/**
		 * Returns a valid map of canonicalid -> grant. Returns null if an error occurred.
		 */
		@Override
		public Map<String, Integer> apply(AccessControlList srcList) {
			HashMap<String, Integer> aclMap = new HashMap<String, Integer>();
			String canonicalId = null;
			if(srcList == null) {
				//Nothing to do
				return aclMap;
			}
			Grantee grantee = null;
			CanonicalUser canonicalUser = null;
			Group group = null;
			String email;
			for(Grant g: srcList.getGrants()) {
				grantee = g.getGrantee();
				if(grantee == null || Strings.isNullOrEmpty(g.getPermission())) {
					//Invalid message. grant. No grantee.
					return null;
				} else {
					canonicalUser = grantee.getCanonicalUser();
					group = grantee.getGroup();
					email = grantee.getEmailAddress();
				}
				
				if(canonicalUser != null && !Strings.isNullOrEmpty(canonicalUser.getID())) {
					//CanonicalId
					try {
						//Check validity of the canonicalId
						canonicalId = Accounts.lookupAccountByCanonicalId(canonicalUser.getID()).getCanonicalId();
					} catch (AuthException e) {
						//For legacy support, also check the account Id. Euca used to use AccountId instead of canoncialId.
						try {
							canonicalId = Accounts.lookupAccountById(canonicalUser.getID()).getCanonicalId();
						} catch(AuthException ex) {
							//Neither canonical Id nor account id worked
							return null;							
						}
					}										
				} else if(!Strings.isNullOrEmpty(email)) {
					//Email
					try {
						canonicalId = Accounts.lookupUserByEmailAddress(email).getAccount().getCanonicalId();
					} catch (AuthException e) {
						//Invalid canonical Id.
						return null;
					}
				} else if(group != null && !Strings.isNullOrEmpty(group.getUri())) {
					try {
						ObjectStorageProperties.S3_GROUP foundGroup = AclUtils.getGroupFromUri(group.getUri());
						if(foundGroup == null) {
							throw new NoSuchElementException("URI: " + group.getUri() + " not found in group map");
						}					
					} catch(NoSuchElementException e) {
						//Invalid group name
						LOG.warn("Invalid group name when trying to map ACL grantee: " + g.getGrantee().getGroup().getUri());
						return null;
					}
					
					//Group URI, use as canonicalId for now.
					canonicalId = group.getUri();
				}
				
				if(canonicalId == null) {
					return null;
				} else {
					//Add permission to existing grant if exists, or create a new one
					int oldGrant = (aclMap.containsKey(canonicalId) ? aclMap.get(canonicalId) : 0);
					int newGrant = BitmapGrant.add(ObjectStorageProperties.Permission.valueOf(g.getPermission().toUpperCase()), oldGrant);
					if(newGrant != 0) {
						aclMap.put(canonicalId, newGrant);
					} else {
						//skip no-op grants
					}
				}
			}
			return aclMap;
		}
	}
	
	/**
	 * Converts from BitmapGrant to 1 or more msg Grants (up to 3).
	 * Does not set the grantee on the grants, just the permission(s).
	 * 
	 * Represents the grant(s) for a single canonicalId/group
	 */
	protected enum AccountGrantsFromBitmap implements Function<Integer, List<ObjectStorageProperties.Permission>> {
		INSTANCE;
		
		@Override
		public List<ObjectStorageProperties.Permission> apply(Integer srcBitmap) {
			ArrayList<ObjectStorageProperties.Permission> permissions = new ArrayList<ObjectStorageProperties.Permission>();
			if(srcBitmap == null) {
				return permissions;
			}				
			
			if(BitmapGrant.allows(ObjectStorageProperties.Permission.FULL_CONTROL, srcBitmap)) {
				permissions.add(ObjectStorageProperties.Permission.FULL_CONTROL);
			} else {
				
				int i = 0;
				if(BitmapGrant.allows(ObjectStorageProperties.Permission.READ, srcBitmap)) {
					permissions.add(ObjectStorageProperties.Permission.READ);
				}
				
				if(BitmapGrant.allows(ObjectStorageProperties.Permission.WRITE, srcBitmap)) {
					permissions.add(ObjectStorageProperties.Permission.WRITE);
				}

				if(BitmapGrant.allows(ObjectStorageProperties.Permission.READ_ACP, srcBitmap)) {
					permissions.add(ObjectStorageProperties.Permission.READ_ACP);
				}
				
				if(BitmapGrant.allows(ObjectStorageProperties.Permission.WRITE_ACP, srcBitmap)) {
					permissions.add(ObjectStorageProperties.Permission.WRITE_ACP);
				}
			}
			return permissions;
		}
	}
	
	/**
	 * Implements the mapping of perms to the bitmap
	 * Mapping: Integer => read,write,readAcp,writeAcp in least significant bits
	 * Example:
	 * read = 8, write = 4, ...
	 * 
	 * @author zhill
	 *
	 */
	protected enum BitmapGrant {
		INSTANCE;
		
		private static final int readMask = 8; //4th bit from right
		private static final int writeMask = 4; //3rd bit 
		private static final int readACPMask = 2; //2nd bit
		private static final int writeACPMask = 1; //1st bit
	
		/**
		 * Does this grant allow the requested permission?
		 * @param perm
		 * @return
		 */
		public static boolean allows(ObjectStorageProperties.Permission perm, int mapValue) {			
			switch(perm) {
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
		 * @param perm
		 */
		public static int translateToBitmap(ObjectStorageProperties.Permission perm) {
			switch(perm) {
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
		 * Add the specified permission, non-destructive. Returns a new bitmap that
		 * adds the requested permission to the oldAclBitmap
		 * @param perm
		 */
		public static int add(ObjectStorageProperties.Permission perm, int oldAclBitmap) {
			switch(perm) {
			case FULL_CONTROL:
				return (int)(oldAclBitmap | readMask | writeMask | readACPMask | writeACPMask);
			case READ:
				return (int)(oldAclBitmap | readMask);
			case WRITE:
				return (int)(oldAclBitmap | writeMask);
			case READ_ACP:
				return (int)(oldAclBitmap | readACPMask);
			case WRITE_ACP:
				return (int)(oldAclBitmap | writeACPMask);			
			}
			return 0;
		}
		
		/**
		 * Returns a log-friendly string of the map in b
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
	
