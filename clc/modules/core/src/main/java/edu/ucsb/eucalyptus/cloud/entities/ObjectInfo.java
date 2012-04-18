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
*    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.WalrusProperties;
import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;
import edu.ucsb.eucalyptus.msgs.Group;
import edu.ucsb.eucalyptus.msgs.MetaDataEntry;

@Entity(optimisticLock=org.hibernate.annotations.OptimisticLockType.NONE)
@javax.persistence.Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "Objects" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ObjectInfo extends AbstractPersistent implements Comparable { 
    @Column( name = "owner_id" )
    private String ownerId;

    @Column( name = "object_key" )
    private String objectKey;

    @Column( name = "bucket_name" )
    private String bucketName;

    @Column( name = "object_name" )
    private String objectName;

    @Column(name="global_read")
    private Boolean globalRead;

    @Column(name="global_write")
    private Boolean globalWrite;

    @Column(name="global_read_acp")
    private Boolean globalReadACP;

    @Column(name="global_write_acp")
    private Boolean globalWriteACP;

    @OneToMany( cascade = CascadeType.ALL )
    @JoinTable(
            name = "object_has_grants",
            joinColumns = { @JoinColumn( name = "object_id" ) },
            inverseJoinColumns = @JoinColumn( name = "grant_id" )
    )
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
    private List<GrantInfo> grants = new ArrayList<GrantInfo>();

    @Column(name="etag")
    private String etag;

    @Column(name="last_modified")
    private Date lastModified;

    @Column(name="size")
    private Long size;

    @Column(name="storage_class")
    private String storageClass;

    @OneToMany( cascade = CascadeType.ALL )
    @JoinTable(
            name = "object_has_metadata",
            joinColumns = { @JoinColumn( name = "object_id" ) },
            inverseJoinColumns = @JoinColumn( name = "metadata_id" )
    )
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
    @Column(name="metadata")
    private List<MetaDataInfo> metaData = new ArrayList<MetaDataInfo>();

    @Column(name="content_type")
    private String contentType;

    @Column(name="content_disposition")
    private String contentDisposition;

    @Column(name="is_deleted")
    private Boolean deleted;
    
    @Column(name="version_id")
    private String versionId;

    @Column(name="is_last")
    private Boolean last;
 
    private static Logger LOG = Logger.getLogger( ObjectInfo.class );

    public ObjectInfo() {
    }

    public ObjectInfo(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
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

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }



    public boolean canWrite(String userId) {
        if(deleted) {
        	return (ownerId.equals(userId));
        }
       
        if (globalWrite) {
            return true;
        }
        if (ownerId.equals(userId)) {
          return true;
        }
        for (GrantInfo grantInfo: grants) {
            if (grantInfo.getUserId().equals(userId)) {
                if (grantInfo.canWrite()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canRead(User user) {
      return user.isSystemAdmin()||canRead(user.getName());
    }

    public boolean canRead(String userId) {
        if(deleted) {
       	    return (ownerId.equals(userId));
        }

        if (globalRead) {
            return true;
        }
        if (ownerId.equals(userId)) {
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

        return false;
    }

    public boolean canWriteACP(String userId) {
        if (globalWriteACP) {
            return true;
        }
        if (ownerId.equals(userId)) {
          return true;
        }
        
        for (GrantInfo grantInfo: grants) {
            if (grantInfo.getUserId().equals(userId)) {
                if (grantInfo.isWriteACP()) {
                    return true;
                }
            }
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
                    }
                    foundGrant = grant;
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

    public void replaceMetaData(List<MetaDataEntry>metaDataEntries) {
        metaData = new ArrayList<MetaDataInfo>();
        for (MetaDataEntry metaDataEntry: metaDataEntries) {
            MetaDataInfo metaDataInfo = new MetaDataInfo();
            metaDataInfo.setObjectName(objectName);
            metaDataInfo.setName(metaDataEntry.getName());
            metaDataInfo.setValue(metaDataEntry.getValue());
            metaData.add(metaDataInfo);
        }
    }

    public void returnMetaData(List<MetaDataEntry>metaDataEntries) {
        for (MetaDataInfo metaDataInfo: metaData) {
            MetaDataEntry metaDataEntry = new MetaDataEntry();
            metaDataEntry.setName(metaDataInfo.getName());
            metaDataEntry.setValue(metaDataInfo.getValue());
            metaDataEntries.add(metaDataEntry);
        }
    }

    public List<MetaDataInfo> cloneMetaData() {
        ArrayList<MetaDataInfo> metaDataInfos = new ArrayList<MetaDataInfo>();
        for(MetaDataInfo metaDataInfo : metaData) {
            metaDataInfos.add(new MetaDataInfo(metaDataInfo));
        }
        return metaDataInfos;
    }

    public List<MetaDataInfo> getMetaData() {
        return metaData;
    }

    public void setMetaData(List<MetaDataInfo> metaData) {
        this.metaData = metaData;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
    
    public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public int compareTo(Object o) {
        return this.objectKey.compareTo(((ObjectInfo)o).getObjectKey());
    }

	
	public Boolean getLast() {
		return last;
	}

	public void setLast(Boolean last) {
		this.last = last;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bucketName == null) ? 0 : bucketName.hashCode());
		result = prime * result
				+ ((objectKey == null) ? 0 : objectKey.hashCode());
		result = prime * result
				+ ((versionId == null) ? 0 : versionId.hashCode());
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
		ObjectInfo other = (ObjectInfo) obj;
		if (bucketName == null) {
			if (other.bucketName != null)
				return false;
		} else if (!bucketName.equals(other.bucketName))
			return false;
		if (objectKey == null) {
			if (other.objectKey != null)
				return false;
		} else if (!objectKey.equals(other.objectKey))
			return false;
		if (versionId == null) {
			if (other.versionId != null)
				return false;
		} else if (!versionId.equals(other.versionId))
			return false;
		return true;
	}
}
