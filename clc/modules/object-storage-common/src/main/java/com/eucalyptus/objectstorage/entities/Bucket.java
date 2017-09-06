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

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;

@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "buckets")
public class Bucket extends S3AccessControlledEntity<BucketState> implements Comparable {
  /**
     *
     */
  private static final long serialVersionUID = 1L;

  @Column(name = "bucket_name", unique = true, nullable = true)
  private String bucketName; // The bucket name as seen by users via S3 API

  @Column(name = "bucket_uuid", unique = true, nullable = false)
  private String bucketUuid; // The bucket id used by the backend to ensure uniqueness for lifecycle

  @Column(name = "bucket_location")
  private String location;

  // Is Bucket logging enabled?
  @Column(name = "logging_enabled")
  private Boolean loggingEnabled;

  // If logging enabled, this is the target bucket for logs
  @Column(name = "target_bucket")
  private String targetBucket;

  // If logging enabled, this is the prefix for log objects
  @Column(name = "target_prefix")
  private String targetPrefix;

  @Column(name = "versioning")
  @Enumerated(EnumType.STRING)
  private VersioningStatus versioning;

  // Needed for enforcing IAM size quotas, to prevent having to scan all objects
  @Column(name = "bucket_size")
  private Long bucketSize;

  @Column(name = "policy")
  @Type(type="text")
  private String policy;

  @PrePersist
  public void checkPrePersist() {
    if (this.getState() == null) {
      throw new RuntimeException("Unspecified state");
    }

    if (versioning == null) {
      versioning = VersioningStatus.Disabled;
    }

    if (bucketSize == null) {
      bucketSize = 0L;
    }

    if (bucketUuid == null) {
      genIds(this.getBucketName());
    }
  }

  /**
   * Returns an initialized Bucket ready for persistence
   *
   * @param name
   * @param canonicalId
   * @param displayName
   * @param iamUserId
   * @param acl
   * @param location
   * @return
   */
  public static Bucket getInitializedBucket(@Nonnull String name, @Nonnull String canonicalId, @Nonnull String displayName,
      @Nonnull String iamUserId, @Nonnull String acl, @Nonnull String location) {
    Bucket newBucket = new Bucket(name);
    newBucket.setOwnerCanonicalId(canonicalId);
    newBucket.setOwnerDisplayName(displayName);
    newBucket.setOwnerIamUserId(iamUserId);
    newBucket.setBucketSize(0L);
    newBucket.setAcl(acl);
    newBucket.setLocation(location);
    newBucket.setLoggingEnabled(false);
    newBucket.setState(BucketState.creating);
    newBucket.setVersioning(ObjectStorageProperties.VersioningStatus.Disabled);
    newBucket.genIds(name);
    return newBucket;
  }

  /**
   * Returns and initialized Bucket ready for persistence Expects a fully configured AccessControlPolicy with populated owner canonicalId and
   * displayName
   *
   * @param name
   * @param iamUserId
   * @param acp
   * @param location
   * @return
   */
  public static Bucket getInitializedBucket(@Nonnull String name, @Nonnull String iamUserId, @Nonnull AccessControlPolicy acp,
      @Nonnull String location) {
    if (acp.getOwner() == null || acp.getOwner().getID() == null || acp.getOwner().getDisplayName() == null) {
      throw new IllegalArgumentException("AccessControlPolicy must include full owner id and name");
    }

    Bucket newBucket = new Bucket(name);
    newBucket.setOwnerCanonicalId(acp.getOwner().getID());
    newBucket.setOwnerDisplayName(acp.getOwner().getDisplayName());
    newBucket.setOwnerIamUserId(iamUserId);
    newBucket.setBucketSize(0L);
    newBucket.setAcl(acp);
    newBucket.setLocation(location);
    newBucket.setLoggingEnabled(false);
    newBucket.setState(BucketState.creating);
    newBucket.setVersioning(ObjectStorageProperties.VersioningStatus.Disabled);
    newBucket.genIds(name);
    return newBucket;
  }

  public String getBucketUuid() {
    return this.bucketUuid;
  }

  /**
   * Sets the bucket UUID directly. INTERNAL USE only!
   *
   * @param bucketUuid
   */
  void setBucketUuid(String bucketUuid) {
    this.bucketUuid = bucketUuid;
  }

  private void genIds(@Nonnull String bucketName) {
    setBucketUuid(UUID.randomUUID().toString());
  }

  public boolean stateStillValid(int stateTimeoutSeconds) {
    if (getState() != null && !BucketState.creating.equals(getState())) {
      // extant and deleting states are always valid
      return true;
    } else if (getState() == null) {
      return false;
    } else {
      try {
        return (System.currentTimeMillis() - this.getLastUpdateTimestamp().getTime()) <= ((long) stateTimeoutSeconds * 1000l);
      } catch (Throwable e) {
        return true; // Err on side of keeping the bucket rather than flushing it if there is an error in lookup.
      }
    }
  }

  /**
   * This is the method to be used to generate example bucket entities for searches
   *
   * @param bucketUuid
   * @return
   */
  public Bucket withUuid(String bucketUuid) {
    this.setBucketUuid(bucketUuid);
    return this;
  }

  /**
   * Should *not* be used for modifying the state of a real record. Only for search examples.
   * <p/>
   * Modifies the state but does not update the 'lastState'. This is intended for search operations where an example object is needed.
   *
   * @param s
   * @return
   */
  public Bucket withState(BucketState s) {
    this.setState(s);
    this.setLastState(null);
    return this;
  }

  public Long getBucketSize() {
    return bucketSize;
  }

  public void setBucketSize(Long bucketSize) {
    this.bucketSize = bucketSize;
  }

  public String generateObjectVersionId() {
    if (ObjectStorageProperties.VersioningStatus.Enabled.equals(this.getVersioning())) {
      return UUID.randomUUID().toString().replaceAll("-", "");
    } else {
      return ObjectStorageProperties.NULL_VERSION_ID;
    }
  }

  @Override
  public String getResourceFullName() {
    return getBucketName();
  }

  public Bucket() {}

  public Bucket(@Nonnull String bucketName) {
    this.bucketName = bucketName;
  }

  public Bucket(String ownerId, String ownerIamUserId, String bucketName) {
    this.genIds(bucketName);
    setOwnerCanonicalId(ownerId);
    setOwnerIamUserId(ownerIamUserId);
  }

  public String getBucketName() {
    return this.bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public boolean hasLoggingPerms() {
    // Logging requires write and readACP
    return this.can(ObjectStorageProperties.Permission.READ_ACP, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString())
        && this.can(ObjectStorageProperties.Permission.WRITE, ObjectStorageProperties.S3_GROUP.LOGGING_GROUP.toString());
  }

  public boolean isOwnedBy(String canonicalId) {
    return this.getOwnerCanonicalId() != null && this.getOwnerCanonicalId().equals(canonicalId);
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
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

  public VersioningStatus getVersioning() {
    return versioning;
  }

  public void setVersioning(VersioningStatus versioning) {
    this.versioning = versioning;
  }

  public String getPolicy( ) {
    return policy;
  }

  public void setPolicy( final String policy ) {
    this.policy = policy;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bucketUuid == null) ? 0 : bucketUuid.hashCode());
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
    Bucket other = (Bucket) obj;
    if (bucketUuid == null) {
      if (other.bucketUuid != null)
        return false;
    } else if (!bucketUuid.equals(other.bucketUuid)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[BucketName: " + this.getBucketName() + " , BucketUuid: " + this.getBucketUuid() + " , State: " + this.getState() + " , Size: "
        + this.getBucketSize() + ", Location: " + this.getLocation() + " , VersioningState: " + this.getVersion() + " , LoggingEnabled: "
        + this.getLoggingEnabled() + " , AclString: " + this.getAcl() + "]";
  }

  public BucketListEntry toBucketListEntry() {
    return new BucketListEntry(this.getBucketName(), DateFormatter.dateToListingFormattedString(this.getCreationTimestamp()));
  }

  @Override
  public int compareTo(Object o) {
    if (o instanceof Bucket) {
      Bucket other = (Bucket) o;
      return this.getBucketName().compareTo(((Bucket) o).getBucketName());
    }
    return 0;
  }
}
