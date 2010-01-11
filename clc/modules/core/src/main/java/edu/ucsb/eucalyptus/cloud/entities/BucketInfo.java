/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;
import edu.ucsb.eucalyptus.msgs.Group;
import edu.ucsb.eucalyptus.util.UserManagement;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.util.WalrusProperties;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "Buckets" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class BucketInfo {
	@Id
	@GeneratedValue
	@Column( name = "bucket_id" )
	private Long id = -1l;

	@Column( name = "owner_id" )
	private String ownerId;

	@Column( name = "bucket_name", unique=true )
	private String bucketName;

	@Column( name = "bucket_creation_date" )
	private Date creationDate;

	@Column(name="global_read")
	private Boolean globalRead;

	@Column(name="global_write")
	private Boolean globalWrite;

	@Column(name="global_read_acp")
	private Boolean globalReadACP;

	@Column(name="global_write_acp")
	private Boolean globalWriteACP;

	@Column(name="bucket_size")
	private Long bucketSize;

	@Column(name="bucket_location")
	private String location;

	@Column(name="hidden")
	private Boolean hidden;

	@Column(name="logging_enabled")
	private Boolean loggingEnabled;

	@Column(name="target_bucket")
	private String targetBucket;

	@Column(name="target_prefix")
	private String targetPrefix;

	@Column(name="versioning")
	private String versioning;

	@OneToMany( cascade = CascadeType.ALL )
	@JoinTable(
			name = "bucket_has_grants",
			joinColumns = { @JoinColumn( name = "bucket_id" ) },
			inverseJoinColumns = @JoinColumn( name = "grant_id" )
	)
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	private List<GrantInfo> grants = new ArrayList<GrantInfo>();

	public BucketInfo() {
	}

	public BucketInfo(String bucketName) {
		this.bucketName = bucketName;
	}

	public BucketInfo(String ownerId, String bucketName, Date creationDate) {
		this.ownerId = ownerId;
		this.bucketName = bucketName;
		this.creationDate = creationDate;
	}

	public String getBucketName()
	{
		return this.bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public Date getCreationDate() {
		return creationDate;
	}
	public boolean isGlobalRead() {
		return globalRead;
	}

	public void setGlobalRead(Boolean globalRead) {
		this.globalRead = globalRead;
	}

	public boolean isGlobalWrite() {
		return globalWrite;
	}

	public void setGlobalWrite(Boolean globalWrite) {
		this.globalWrite = globalWrite;
	}

	public boolean isGlobalReadACP() {
		return globalReadACP;
	}

	public void setGlobalReadACP(Boolean globalReadACP) {
		this.globalReadACP = globalReadACP;
	}

	public boolean isGlobalWriteACP() {
		return globalWriteACP;
	}

	public void setGlobalWriteACP(Boolean globalWriteACP) {
		this.globalWriteACP = globalWriteACP;
	}

	public List<GrantInfo> getGrants() {
		return grants;
	}

	public void setGrants(List<GrantInfo> grants) {
		this.grants = grants;
	}

	public boolean canWrite(String userId) {
		if (globalWrite) {
			return true;
		}

		for (GrantInfo grantInfo: grants) {
			if (grantInfo.getUserId().equals(userId)) {
				if (grantInfo.canWrite()) {
					return true;
				}
			}
		}
		if(UserManagement.isAdministrator(userId)) {
			return true;
		}

		return false;
	}

	public boolean canRead(String userId) {
		if (globalRead) {
			return true;
		}

		for (GrantInfo grantInfo: grants) {
			if(grantInfo.getGrantGroup() != null) {
				String groupUri = grantInfo.getGrantGroup();
				if(groupUri.equals(WalrusProperties.AUTHENTICATED_USERS_GROUP))
					return true;
			}

		}

		for (GrantInfo grantInfo: grants) {
			if (grantInfo.getUserId().equals(userId)) {
				if (grantInfo.canRead()) {
					return true;
				}
			}
		}

		if(UserManagement.isAdministrator(userId)) {
			return true;
		}

		return false;
	}

	public boolean canWriteACP(String userId) {
		if (globalWriteACP) {
			return true;
		}

		for (GrantInfo grantInfo: grants) {
			if (grantInfo.getUserId().equals(userId)) {
				if (grantInfo.isWriteACP()) {
					return true;
				}
			}
		}
		if(UserManagement.isAdministrator(userId)) {
			return true;
		}

		return false;
	}

	public boolean canReadACP(String userId) {
		if(ownerId.equals(userId)) {
			//owner can always acp
			return true;
		} else if (globalReadACP) {
			return true;
		} else {
			for (GrantInfo grantInfo: grants) {
				if(grantInfo.getUserId().equals(userId) && grantInfo.canReadACP()) {
					return true;
				}
			}
		}
		if(UserManagement.isAdministrator(userId)) {
			return true;
		}
		return false;
	}

	public void resetGlobalGrants() {
		globalRead = globalWrite = globalReadACP = globalWriteACP = false;
	}

	public  void addGrants(String ownerId, List<GrantInfo>grantInfos, AccessControlListType accessControlList) {
		ArrayList<Grant> grants = accessControlList.getGrants();
		Grant foundGrant = null;
		globalRead = globalReadACP = false;
		globalWrite = globalWriteACP = false;
		if (grants.size() > 0) {
			for (Grant grant: grants) {
				String permission = grant.getPermission();
				if (permission.equals("aws-exec-read")) {
					globalRead = globalReadACP = false;
					globalWrite = globalWriteACP = false;
					foundGrant = grant;
				}   else if (permission.equals("public-read")) {
					globalRead = globalReadACP = true;
					globalWrite = globalWriteACP = false;
					foundGrant = grant;
				}   else if (permission.equals("public-read-write")) {
					globalRead = globalReadACP = true;
					globalWrite = globalWriteACP = true;
					foundGrant = grant;
				}   else if (permission.equals("authenticated-read")) {
					globalRead = globalReadACP = false;
					globalWrite = globalWriteACP = false;
					foundGrant = grant;
				} else if(grant.getGrantee().getGroup() != null) {
					String groupUri = grant.getGrantee().getGroup().getUri();
					if(groupUri.equals(WalrusProperties.ALL_USERS_GROUP)) {
						if(permission.equals("FULL_CONTROL"))
							globalRead = globalReadACP = globalWrite = globalWriteACP = true;
						else if(permission.equals("READ"))
							globalRead = true;
						else if(permission.equals("READ_ACP"))
							globalReadACP = true;
						else if(permission.equals("WRITE"))
							globalWrite = true;
						else if(permission.equals("WRITE_ACP"))
							globalWriteACP = true;
						foundGrant = grant;
					}
				}
			}
		}
		if(foundGrant != null) {
			grants.remove(foundGrant);
		}
		GrantInfo.addGrants(ownerId, grantInfos, accessControlList);
	}

	public void readPermissions(List<Grant> grants) {
		if(globalRead && globalReadACP && globalWrite && globalWriteACP) {
			grants.add(new Grant(new Grantee(new Group(WalrusProperties.ALL_USERS_GROUP)), "FULL_CONTROL"));
			return;
		}
		if(globalRead) {
			grants.add(new Grant(new Grantee(new Group(WalrusProperties.ALL_USERS_GROUP)), "READ"));
		}
		if(globalReadACP) {
			grants.add(new Grant(new Grantee(new Group(WalrusProperties.ALL_USERS_GROUP)), "READ_ACP"));
		}
		if(globalWrite) {
			grants.add(new Grant(new Grantee(new Group(WalrusProperties.ALL_USERS_GROUP)), "WRITE"));
		}
		if(globalWriteACP) {
			grants.add(new Grant(new Grantee(new Group(WalrusProperties.ALL_USERS_GROUP)), "WRITE_ACP"));
		}
	}

	public void readAllUsers(String permission) {
		if("FULL_CONTROL".equals(permission)) {
			globalRead = globalWrite = globalReadACP = globalWriteACP = true;
		} else if("READ".equals(permission)) {
			globalRead = true;
		} else if("WRITE".equals(permission)) {
			globalWrite = true;
		} else if("READ_ACP".equals(permission)) {
			globalReadACP = true;
		} else if("WRITE_ACP".equals(permission)) {
			globalWriteACP = true;
		}
	}

	public void addGrants(List<Grant> newGrants) {
		for(Grant grant : newGrants) {
			boolean found = false;
			for(GrantInfo gInfo : grants) {
				if(grant.getGrantee().getCanonicalUser() != null) {
					if(grant.getGrantee().getCanonicalUser().equals(gInfo.getUserId()))
						found = true;
				} else {
					if(grant.getGrantee().getGroup().equals(gInfo.getGrantGroup()))
						found = true;
					if(WalrusProperties.ALL_USERS_GROUP.equals(gInfo.getGrantGroup())) {
						readAllUsers(grant.getPermission());
						break;
					}
				}
				if(found) {
					gInfo.setPermission(grant.getPermission());
					break;
				}
			}

			if(!found) {
				GrantInfo grantInfo = new GrantInfo();
				if(grant.getGrantee().getCanonicalUser() != null) {
					grantInfo.setUserId(grant.getGrantee().getCanonicalUser().getDisplayName());
				} else {
					grantInfo.setGrantGroup(grant.getGrantee().getGroup().getUri());
					if(WalrusProperties.ALL_USERS_GROUP.equals(grantInfo.getGrantGroup())) {
						readAllUsers(grant.getPermission());
						continue;
					}
				}
				String permission = grant.getPermission();
				grantInfo.setPermission(permission);
				grants.add(grantInfo);
			}
		}
	}

	public boolean hasLoggingPerms() {
		boolean hasReadACPPerms = false;
		boolean hasWritePerms = false;
		for(GrantInfo grantInfo : grants) {
			if(WalrusProperties.LOGGING_GROUP.equals(grantInfo.getGrantGroup())) {
				if(grantInfo.canReadACP())
					hasReadACPPerms = true;
				else if(grantInfo.canWrite())
					hasWritePerms = true;
			}
			if(hasReadACPPerms && hasWritePerms)
				return true;
		}
		return false;
	}

	public Long getBucketSize() {
		return bucketSize;
	}

	public void setBucketSize(Long bucketSize) {
		this.bucketSize = bucketSize;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getLoggingEnabled() {
		return loggingEnabled;
	}

	public void setLoggingEnabled(Boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	public String getTargetBucket() {
		return targetBucket;
	}

	public void setTargetBucket(String targetBucket) {
		this.targetBucket = targetBucket;
	}

	public String getTargetPrefix() {
		return targetPrefix;
	}

	public void setTargetPrefix(String targetPrefix) {
		this.targetPrefix = targetPrefix;
	}

	public String getVersioning() {
		return versioning;
	}

	public void setVersioning(String versioning) {
		this.versioning = versioning;
	}

	public boolean isVersioningEnabled() {
		return WalrusProperties.VersioningStatus.Enabled.toString().equals(versioning);	
	}
	
	public boolean isVersioningDisabled() {
		return WalrusProperties.VersioningStatus.Disabled.toString().equals(versioning);	
	}
	
	public boolean isVersioningSuspended() {
		return WalrusProperties.VersioningStatus.Suspended.toString().equals(versioning);	
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((bucketName == null) ? 0 : bucketName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BucketInfo other = (BucketInfo) obj;
		if (bucketName == null) {
			if (other.bucketName != null)
				return false;
		} else if (!bucketName.equals(other.bucketName))
			return false;
		return true;
	}	
}
