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

package com.eucalyptus.objectstorage.entities;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.exceptions.s3.AccountProblemException;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.Part;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

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

//TODO: make the a child-class of ObjectEntity
@Entity
@OptimisticLocking(type = OptimisticLockType.NONE)
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "parts")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class PartEntity extends S3AccessControlledEntity<ObjectState> implements Comparable {
    @Column(name = "object_key")
    private String objectKey;

    @NotFound(action = NotFoundAction.EXCEPTION)
    @ManyToOne(optional = false, targetEntity = Bucket.class, fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_bucket")
    @JoinColumn(name = "bucket_fk")
    private Bucket bucket;

    @Index(name = "IDX_part_uuid")
    @Column(name = "part_uuid", unique = true, nullable = false)
    private String partUuid; //The a uuid for this specific object content & request

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "storage_class", nullable = false)
    private String storageClass;

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

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "part_number")
    private Integer partNumber;

    private static Logger LOG = Logger.getLogger(PartEntity.class);

    public PartEntity() {
        super();
    }

    public PartEntity(Bucket parentBucket, String objectKey, String uploadId) {
        super();
        this.bucket = parentBucket;
        this.uploadId = uploadId;
        this.objectKey = objectKey;
    }

    public PartEntity withBucket(Bucket parentBucket) {
        this.setBucket(parentBucket);
        return this;
    }

    public PartEntity withKey(String key) {
        this.setObjectKey(key);
        return this;
    }

    public PartEntity withUuid(String uuid) {
        this.setPartUuid(uuid);
        return this;
    }

    public PartEntity withUploadId(String uploadId) {
        this.setUploadId(uploadId);
        return this;
    }

    public PartEntity withPartNumber(int partNumber) {
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
    public PartEntity withState(ObjectState s) {
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
        if (this.partUuid == null) {
            //generate a new one
            this.partUuid = generateInternalKey(this.objectKey);
        }

    }

    /**
     * Initialize this as a new object entity representing an object to PUT
     *
     * @param bucket
     * @param objectKey
     * @param usr
     */
    public static PartEntity newInitializedForCreate(@Nonnull Bucket bucket,
                                                     @Nonnull String objectKey,
                                                     @Nonnull String uploadId,
                                                     @Nonnull Integer partNumber,
                                                     @Nonnull long contentLength,
                                                     @Nonnull User usr) throws Exception {
        PartEntity entity = new PartEntity(bucket, objectKey, uploadId);
        entity.setPartUuid(generateInternalKey(objectKey));

        try {
            entity.setOwnerCanonicalId(usr.getAccount().getCanonicalId());
            entity.setOwnerDisplayName(usr.getAccount().getName());
        } catch (AuthException e) {
            throw new AccountProblemException();
        }
        entity.setPartNumber(partNumber);
        entity.setUploadId(uploadId);
        entity.setOwnerIamUserId(usr.getUserId());
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
        return getBucket().getBucketName() + "/" + getObjectKey() + "?uploadId=" + this.uploadId + "&partNumber=" + this.partNumber;
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

    public int compareTo(Object o) {
        return this.objectKey.compareTo(((PartEntity) o).getObjectKey());
    }

    public boolean isPending() {
        return (getObjectModifiedTimestamp() == null);
    }

    public String getPartUuid() {
        return partUuid;
    }

    public void setPartUuid(String partUuid) {
        this.partUuid = partUuid;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
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
                + ((partUuid == null) ? 0 : partUuid.hashCode());
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
        if (objectKey == null) {
            if (other.objectKey != null)
                return false;
        } else if (!objectKey.equals(other.objectKey))
            return false;
        if (partUuid == null) {
            if (other.partUuid != null)
                return false;
        } else if (!partUuid.equals(other.partUuid))
            return false;
        if (uploadId == null) {
            if (other.uploadId != null)
                return false;
        } else if (!uploadId.equals(other.uploadId))
            return false;
        if (partNumber == null) {
            if (other.partNumber != null)
                return false;
        } else if (!partNumber.equals(other.partNumber))
            return false;


        return true;
    }

    /**
     * Return a ListEntry for this entity
     *
     * @return
     */
    public Part toPartListEntry() {
        Part e = new Part();
        e.setEtag("\"" + this.geteTag() + "\"");
        e.setPartNumber(this.getPartNumber());
        e.setLastModified(this.getObjectModifiedTimestamp());
        e.setSize(this.getSize());
        return e;
    }
}
