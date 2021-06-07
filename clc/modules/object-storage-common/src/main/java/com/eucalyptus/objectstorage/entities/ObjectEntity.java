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

package com.eucalyptus.objectstorage.entities;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.DeleteMarkerEntry;
import com.eucalyptus.storage.msgs.s3.KeyEntry;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.VersionEntry;
import com.google.common.collect.Maps;

@Entity
@OptimisticLocking(type = OptimisticLockType.NONE)
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "objects", indexes = {
    @Index(name = "IDX_object_key", columnList = "object_key"),
    @Index(name = "IDX_object_uuid", columnList = "object_uuid"),
    @Index(name = "IDX_version_id", columnList = "version_id"),
    @Index(name = "IDX_object_bucket_fk", columnList = "bucket_fk"),
    @Index(name = "IDX_object_sort", columnList = "object_key, object_last_modified desc"),
})
public class ObjectEntity extends S3AccessControlledEntity<ObjectState> implements Comparable {
  private static Logger LOG = Logger.getLogger(ObjectEntity.class);

  @Column(name = "object_key")
  private String objectKey;

  @NotFound(action = NotFoundAction.EXCEPTION)
  @ManyToOne(optional = false, targetEntity = Bucket.class, fetch = FetchType.EAGER)
  @ForeignKey(name = "FK_bucket")
  @JoinColumn(name = "bucket_fk")
  private Bucket bucket;

  @Column(name = "object_uuid", unique = true, nullable = false)
  private String objectUuid; // The a uuid for this specific object content & request

  @Column(name = "version_id", nullable = false)
  private String versionId; // VersionId is required to uniquely identify ACLs and auth

  @Column(name = "size", nullable = false)
  private Long size;

  @Column(name = "storage_class", nullable = false)
  private String storageClass;

  @Column(name = "is_delete_marker")
  private Boolean isDeleteMarker; // Indicates this is a delete marker

  @Column(name = "object_last_modified")
  // Distinct from the record modification date, tracks the backend response
  private Date objectModifiedTimestamp;

  @Column(name = "etag")
  private String eTag;

  @Column(name = "is_latest")
  private Boolean isLatest;

  @Column(name = "is_cleanup_required")
  private Boolean isCleanupRequired;

  @Column(name = "creation_expiration")
  private Long creationExpiration; // Expiration time in system/epoch time to guarantee monotonically increasing values

  @Column(name = "upload_id")
  private String uploadId;

  @Column(name = "stored_headers" )
  @Type( type = "text" )
  private String storedHeaders;

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

  /**
   * Sets state only, explicitly nulls 'lastState'. Use ONLY for query examples
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
      // generate a new one
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
  public static ObjectEntity newInitializedForCreate(@Nonnull Bucket bucket, @Nonnull String objectKey, long contentLength, @Nonnull UserPrincipal usr)
      throws Exception {
    ObjectEntity entity = new ObjectEntity(bucket, objectKey, bucket.generateObjectVersionId());
    entity.setObjectUuid(generateInternalKey(objectKey));
    entity.setOwnerCanonicalId(usr.getCanonicalId());
    entity.setOwnerDisplayName(usr.getAccountAlias());
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

  public static ObjectEntity newInitializedForCreate(@Nonnull Bucket bucket, @Nonnull String objectKey, long contentLength,
      @Nonnull UserPrincipal usr, @Nullable Map<String, String> headersToStore) throws Exception {
    ObjectEntity entity = newInitializedForCreate(bucket, objectKey, contentLength, usr);
    entity.setStoredHeaders(headersToStore);
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
    ObjectEntity deleteMarker =
        new ObjectEntity(this.bucket, this.getObjectKey(), this.getBucket().generateObjectVersionId()).withState(ObjectState.creating);
    deleteMarker.setObjectUuid(generateInternalKey(objectKey));
    deleteMarker.setStorageClass("STANDARD");
    deleteMarker.setObjectModifiedTimestamp(new Date());
    deleteMarker.setIsDeleteMarker(true);
    deleteMarker.setSize(0L);
    deleteMarker.markLatest();
    return deleteMarker;
  }

  private static String generateInternalKey(@Nonnull String key) {
    return UUID.randomUUID().toString(); // Use only uuid to ensure key length max met
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

  public void markLatest() {
    setIsLatest(Boolean.TRUE);
    setCleanupRequired(Boolean.TRUE);
  }

  public void setIsLatest(Boolean isLatest) {
    this.isLatest = isLatest;
  }

  public Boolean getCleanupRequired() {
    return isCleanupRequired;
  }

  public void setCleanupRequired(final Boolean cleanupRequired) {
    isCleanupRequired = cleanupRequired;
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

  public Map<String, String> getStoredHeaders() {
    Map<String, String> headersMap = Maps.newHashMap();
    if (storedHeaders == null || "".equals(storedHeaders)) {
      return headersMap;
    }
    JSONObject headersJson = (JSONObject) JSONSerializer.toJSON(this.storedHeaders);
    String key = null;
    Iterator keys = headersJson.keys();
    while (keys.hasNext()) {
      key = (String) keys.next();
      Object value = headersJson.get(key);
      if (value != null) {
        headersMap.put(key, value.toString());
      }
    }
    return headersMap;
  }

  public void setStoredHeaders(Map<String, String> storedHeadersMap) {
    this.storedHeaders = JSONObject.fromObject(storedHeadersMap).toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((objectUuid == null) ? 0 : objectUuid.hashCode());
    result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
    result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
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

  /**
   * Return a ListEntry for this entity
   *
   * @return
   */
  public ListEntry toListEntry(boolean includeOwner) {
    ListEntry e = new ListEntry();
    e.setEtag("\"" + this.geteTag() + "\"");
    e.setKey(this.getObjectKey());
    e.setLastModified(DateFormatter.dateToListingFormattedString(this.getObjectModifiedTimestamp()));
    e.setSize(this.getSize());
    e.setStorageClass(this.getStorageClass());
    if (includeOwner) {
      e.setOwner(new CanonicalUser(this.getOwnerCanonicalId(), this.getOwnerDisplayName()));
    }
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

  @PreUpdate
  protected void preUpdate() {
    if (!Boolean.TRUE.equals(getIsLatest())) {
      setCleanupRequired(null);
    }
  }

  // TODO: add delete marker support. Fix is to use super-type for versioning entry and sub-types for version vs deleteMarker

}
