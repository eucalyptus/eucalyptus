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

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import java.util.Date;

@Entity
@OptimisticLocking(type = OptimisticLockType.NONE)
@PersistenceContext(name="eucalyptus_osg")
@Table( name = "parts" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PartEntity extends AbstractPersistent implements Comparable {
	@Column( name = "object_key" )
    private String objectKey;

    @Column( name = "bucket_name" )
    private String bucketName;
    
    @Index(name = "IDX_uuid")
    @Column(name="uuid", unique=true)
    private String uuid; //The a uuid for this specific object content & request

	@Column(name="size")
    private Long size;

    @Column(name="object_last_modified") //Distinct from the record modification date, tracks the backend response
    private Date objectModifiedTimestamp;

    @Column(name="etag")
    private String eTag;
    
    @Column(name="upload_id")
    private String uploadId;

    @Column(name="part_number")
    private Integer partNumber;

    @Column( name = "owner_canonical_id" )
    protected String ownerCanonicalId;

    @Column( name = "owner_iam_user_id" )
    protected String ownerIamUserId;

    @Column( name = "owner_displayname")
    protected String ownerDisplayName;

    //Display name for IAM user
    @Column( name = "owner_iam_user_displayname" )
    protected String ownerIamUserDisplayName;

    private static Logger LOG = Logger.getLogger( PartEntity.class );
    
    public PartEntity() {}

    public PartEntity(String bucketName, String objectKey, String uploadId, Integer partNumber) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.partNumber = partNumber;
    }

    /**
     * Initialize this as a new entity representing a part uploaded with UploadPart
     * @param bucketName
     * @param objectKey
     * @param requestId
     * @param usr
     */
    public void initializeForCreate(String bucketName, String objectKey, String requestId, long contentLength, User usr) throws Exception {
    	this.setBucketName(bucketName);
    	this.setObjectKey(objectKey);
    	if(this.getUuid() == null) {
    		//Generate a new internal key
    		this.setUuid(generateInternalKey(requestId, objectKey));
    	}
    	this.setObjectModifiedTimestamp(null);
    	this.setSize(contentLength);
    }
    
    public void finalizeCreation(@Nullable Date lastModified, @Nonnull String etag) throws Exception {
    	this.seteTag(etag);
    	if(lastModified != null) {
    		this.setObjectModifiedTimestamp(lastModified);
    	} else {
    		this.setObjectModifiedTimestamp(new Date());
    	}
    }

    public String getResourceFullName() {
        return getBucketName() + "/" + getObjectKey() + " uploadId: " + getUploadId() + " partNumber: " + getPartNumber();
    }

    private static String generateInternalKey(String requestId, String key) {
		return key + "-" + requestId;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public Date getObjectModifiedTimestamp() {
		return objectModifiedTimestamp;
	}

	public void setObjectModifiedTimestamp(Date objectModifiedTimestamp) {
		this.objectModifiedTimestamp = objectModifiedTimestamp;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

	public int compareTo(Object o) {
        return this.objectKey.compareTo(((PartEntity)o).getObjectKey());
    }

	public boolean isPending() {
		return (getObjectModifiedTimestamp() == null);
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getOwnerIamUserId() {
        return ownerIamUserId;
    }

    public void setOwnerIamUserId(String ownerIamUserId) {
        this.ownerIamUserId = ownerIamUserId;
    }

    public String getOwnerIamUserDisplayName() {
        return ownerIamUserDisplayName;
    }

    public void setOwnerIamUserDisplayName(String ownerIamUserDisplayName) {
        this.ownerIamUserDisplayName = ownerIamUserDisplayName;
    }

    public String getOwnerCanonicalId() {
        return ownerCanonicalId;
    }

    public void setOwnerCanonicalId(String ownerCanonicalId) {
        this.ownerCanonicalId = ownerCanonicalId;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
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
				+ ((uploadId == null) ? 0 : uploadId.hashCode());
        result = prime * result
                + ((partNumber == null) ? 0 : partNumber.hashCode());
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
		PartEntity other = (PartEntity) obj;
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
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
        if (uploadId == null) {
            if (other.uploadId != null)
                return false;
        } else if (!uploadId.equals(other.uploadId))
            return false;

        return true;
	}
	
	public static class QueryHelpers {
		
		public static Criterion getNotPendingRestriction() {
			return Restrictions.isNotNull("objectModifiedTimestamp");
		}
		
		/**
		 * The condition to determine if an object record is failed -- where failed means the PUT did not complete
		 * and it was not handled cleanly on failure. e.g. OSG failed before it could finalize the object record
		 * @return
		 */
		public static Criterion getFailedRestriction() {
			return Restrictions.and(Restrictions.isNull("objectModifiedTimestamp"), Restrictions.le("creationTimestamp", getOldestFailedAllowed()));
		}


		/**
		 * Returns timestamp for detecting failed-put records. Any record with created timestamp less than
		 * this value that have not been completed are considered failed.
		 */
		public static Date getOldestFailedAllowed() {
			long now = new Date().getTime();
			//Subtract the failed window hours.
			Integer windowHrs = null;
			try {
				windowHrs = ObjectStorageGatewayGlobalConfiguration.failed_put_timeout_hrs;
			} catch(Exception e) {
				LOG.error("Error getting configured failed put timeout. Using 1970 epoch as fallback", e);
				windowHrs = null;
			}
			
			if(windowHrs == null) {
				return new Date(0); //1970 epoch
			}								
			long windowStart = now - (1000L * 60 * 60 * windowHrs);
			return new Date(windowStart);
		}

        public static Criterion getIsPendingRestriction() {
            return Restrictions.isNull("objectModifiedTimestamp");
        }

    }
		
	/**
	 * Return a ListEntry for this entity
	 * @return
	 */
	public ListEntry toListEntry() {
		ListEntry e = new ListEntry();
		e.setEtag("\"" + this.geteTag() + "\"");
		e.setKey(this.getObjectKey());
		e.setLastModified(OSGUtil.dateToListingFormattedString(this.getObjectModifiedTimestamp()));
		e.setSize(this.getSize());
		e.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
		return e;
	}
	
}
