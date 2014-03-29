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

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.exceptions.s3.AccountProblemException;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.DeleteMarkerEntry;
import com.eucalyptus.storage.msgs.s3.KeyEntry;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.VersionEntry;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@OptimisticLocking(type = OptimisticLockType.NONE)
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "objects")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class ObjectEntity extends S3AccessControlledEntity<ObjectState> implements Comparable {
    @Column(name = "object_key")
    private String objectKey;

    @NotFound(action = NotFoundAction.EXCEPTION)
    @ManyToOne(optional = false, targetEntity = Bucket.class, fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_bucket")
    @JoinColumn(name = "bucket_fk")
    private Bucket bucket;

    @Index(name = "IDX_object_uuid")
    @Column(name = "object_uuid", unique = true, nullable = false)
    private String objectUuid; //The a uuid for this specific object content & request

    @Column(name = "version_id", nullable = false)
    private String versionId; //VersionId is required to uniquely identify ACLs and auth

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "storage_class", nullable = false)
    private String storageClass;

    @Column(name = "is_delete_marker")
    private Boolean isDeleteMarker; //Indicates this is a delete marker 

    @Column(name = "object_last_modified") //Distinct from the record modification date, tracks the backend response
    private Date objectModifiedTimestamp;

    @Column(name = "etag")
    private String eTag;

    @Column(name = "is_latest")
    private Boolean isLatest;

    @Column(name = "creation_expiration")
    private Long creationExpiration; //Expiration time in system/epoch time to guarantee monotonically increasing values

    public Long getCreationExpiration() {
        return creationExpiration;
    }

    public void setCreationExpiration(Long creationExpiration) {
        this.creationExpiration = creationExpiration;
    }

    public Boolean getIsDeleteMarker() {
        return isDeleteMarker;
    }

    public void setIsDeleteMarker(Boolean isDeleteMarker) {
        this.isDeleteMarker = isDeleteMarker;
    }

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "part_number")
    private Integer partNumber;

    private static Logger LOG = Logger.getLogger(ObjectEntity.class);

    public ObjectEntity() {
        super();
    }

    public ObjectEntity(Bucket parentBucket, String objectKey, String versionId) {
        super();
        this.bucket = parentBucket;
        this.objectKey = objectKey;
        this.versionId = versionId;
    }

    public ObjectEntity withBucket(Bucket parentBucket) {
        this.setBucket(parentBucket);
        return this;
    }

    public ObjectEntity withKey(String key) {
        this.setObjectKey(key);
        return this;
    }

    public ObjectEntity withVersionId(String versionId) {
        this.setVersionId(versionId);
        return this;
    }

    public ObjectEntity withUuid(String uuid) {
        this.setObjectUuid(uuid);
        return this;
    }

    public ObjectEntity withUploadId(String uploadId) {
        this.setUploadId(uploadId);
        return this;
    }

    public ObjectEntity withPartNumber(int partNumber) {
        this.setPartNumber(partNumber);
        return this;
    }

    /**
     * Sets state only, explicitly nulls 'lastState'. Use ONLY for
     * query examples
     *
     * @param s
     * @return
     */
    public ObjectEntity withState(ObjectState s) {
        this.setState(s);
        this.setLastState(null);
        return this;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    @PrePersist
    public void ensureFieldsNotNull() {
        if (this.versionId == null) {
            this.versionId = ObjectStorageProperties.NULL_VERSION_ID;
        }

        if (this.isDeleteMarker == null) {
            this.isDeleteMarker = false;
        }

        if (this.objectUuid == null) {
            //generate a new one
            this.generateInternalKey(this.objectKey);
        }

    }

    /**
     * Initialize this as a new object entity representing an object to PUT
     *
     * @param bucket
     * @param objectKey
     * @param usr
     */
    public static ObjectEntity newInitializedForCreate(@Nonnull Bucket bucket, @Nonnull String objectKey, @Nonnull long contentLength, @Nonnull User usr) throws Exception {
        ObjectEntity entity = new ObjectEntity(bucket, objectKey, bucket.generateObjectVersionId());
        entity.setObjectUuid(generateInternalKey(objectKey));

        try {
            entity.setOwnerCanonicalId(usr.getAccount().getCanonicalId());
            entity.setOwnerDisplayName(usr.getAccount().getName());
        } catch (AuthException e) {
            throw new AccountProblemException();
        }

        entity.setOwnerIamUserId(usr.getUserId());
        entity.setOwnerIamUserDisplayName(usr.getName());
        entity.setObjectModifiedTimestamp(null);
        entity.setSize(contentLength);
        entity.setIsLatest(false);
        entity.setStorageClass(ObjectStorageProperties.STORAGE_CLASS.STANDARD.toString());
        entity.updateCreationExpiration();
        entity.setState(ObjectState.creating);
        return entity;
    }

    public void updateCreationExpiration() {
        this.creationExpiration = System.currentTimeMillis() + (1000 * ObjectStorageProperties.OBJECT_CREATION_EXPIRATION_INTERVAL_SEC);
    }

    /**
     * Creates a new 'DeleteMarker' object entity from this object
     *
     * @return
     * @throws Exception
     */
    public ObjectEntity generateNewDeleteMarkerFrom() {
        if (versionId == null) {
            throw new IllegalArgumentException("versionId cannot be null for delete marker generation");
        }

        ObjectEntity deleteMarker = new ObjectEntity(this.bucket, this.getObjectKey(), this.getBucket().generateObjectVersionId()).withState(ObjectState.extant);
        deleteMarker.setObjectUuid(generateInternalKey(objectKey));
        deleteMarker.setStorageClass("STANDARD");
        deleteMarker.setObjectModifiedTimestamp(new Date());
        deleteMarker.setIsDeleteMarker(true);
        deleteMarker.setSize(-1L);
        deleteMarker.setIsLatest(true);
        return deleteMarker;
    }

    private static String generateInternalKey(@Nonnull String key) {
        return UUID.randomUUID().toString() + "-" + key;
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

    @Override
    public String getResourceFullName() {
        return getBucket().getBucketName() + "/" + getObjectKey();
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
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

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public int compareTo(Object o) {
        return this.objectKey.compareTo(((ObjectEntity) o).getObjectKey());
    }

    public boolean isPending() {
        return (getObjectModifiedTimestamp() == null);
    }

    public String getObjectUuid() {
        return objectUuid;
    }

    public void setObjectUuid(String objectUuid) {
        this.objectUuid = objectUuid;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
    }

    public boolean isNullVersioned() {
        return ObjectStorageProperties.NULL_VERSION_ID.equals(this.versionId);
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((objectUuid == null) ? 0 : objectUuid.hashCode());
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
        ObjectEntity other = (ObjectEntity) obj;
        if (bucket == null) {
            if (other.bucket == null)
                return false;
        } else if (!bucket.equals(other.bucket))
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

        if (objectUuid == null) {
            if (other.objectUuid != null)
                return false;
        } else if (!objectUuid.equals(other.objectUuid))
            return false;

        return true;
    }

    public static class QueryHelpers {

        public static Criterion getIsPartRestriction() {
            return Restrictions.isNotNull("partNumber");
        }

        public static Criterion getIsNotPartRestriction() {
            return Restrictions.isNull("partNumber");
        }

        public static Criterion getIsPendingRestriction() {
            return Restrictions.isNull("objectModifiedTimestamp");
        }

        public static Criterion getIsMultipartRestriction() {
            return Restrictions.isNotNull("uploadId");
        }
    }

    /**
     * Return a ListEntry for this entity
     *
     * @return
     */
    public ListEntry toListEntry() {
        ListEntry e = new ListEntry();
        e.setEtag("\"" + this.geteTag() + "\"");
        e.setKey(this.getObjectKey());
        e.setLastModified(DateFormatter.dateToListingFormattedString(this.getObjectModifiedTimestamp()));
        e.setSize(this.getSize());
        e.setStorageClass(this.getStorageClass());
        e.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
        return e;
    }

    /**
     * Return a VersionEntry for this entity
     *
     * @return
     */
    public KeyEntry toVersionEntry() {
        if (!this.isDeleteMarker) {
            VersionEntry e = new VersionEntry();
            e.setEtag("\"" + this.geteTag() + "\"");
            e.setKey(this.getObjectKey());
            e.setVersionId(this.getVersionId());
            e.setLastModified(DateFormatter.dateToListingFormattedString(this.getObjectModifiedTimestamp()));
            e.setSize(this.getSize());
            e.setIsLatest(this.isLatest);
            e.setStorageClass(this.getStorageClass());
            e.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
            return e;
        } else {
            DeleteMarkerEntry e = new DeleteMarkerEntry();
            e.setKey(this.getObjectKey());
            e.setVersionId(this.getVersionId());
            e.setLastModified(DateFormatter.dateToListingFormattedString(this.getObjectModifiedTimestamp()));
            e.setIsLatest(this.isLatest);
            e.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
            return e;
        }

    }
    //TODO: add delete marker support. Fix is to use super-type for versioning entry and sub-types for version vs deleteMarker

}
