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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.UserManagement;
import com.eucalyptus.util.WalrusProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

@Entity
@Table( name = "Objects" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ObjectInfo implements Comparable {
    @Id
    @GeneratedValue
    @Column( name = "object_id" )
    private Long id = -1l;

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
    @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
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
    @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
    @Column(name="metadata")
    private List<MetaDataInfo> metaData = new ArrayList<MetaDataInfo>();

    @Column(name="content_type")
    private String contentType;

    @Column(name="content_disposition")
    private String contentDisposition;

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
        if (globalWrite) {
            return true;
        }

        for (GrantInfo grantInfo: grants) {
            if (grantInfo.getUserId().equals(userId)) {
                if (grantInfo.isWrite()) {
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
                if (grantInfo.isRead()) {
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
                if(grantInfo.getUserId().equals(userId) && grantInfo.isReadACP()) {
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
            metaDataInfos.add(metaDataInfo.clone());
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
    
    public int compareTo(Object o) {
        return this.objectKey.compareTo(((ObjectInfo)o).getObjectKey());
    }    
}
