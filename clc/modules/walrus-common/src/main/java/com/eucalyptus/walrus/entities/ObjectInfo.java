/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.walrus.util.WalrusProperties;

@Entity
@OptimisticLocking(type = OptimisticLockType.NONE)
@PersistenceContext(name = "eucalyptus_walrus")
@Table(name = "Objects")
public class ObjectInfo extends AbstractPersistent {
  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "object_key")
  private String objectKey;

  @Column(name = "bucket_name")
  private String bucketName;

  @Column(name = "object_name")
  private String objectName;

  @Column(name = "etag")
  private String etag;

  @Column(name = "last_modified")
  private Date lastModified;

  @Column(name = "size")
  private Long size;

  @Column(name = "storage_class")
  private String storageClass;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinTable(name = "object_has_metadata", joinColumns = {@JoinColumn(name = "object_id")}, inverseJoinColumns = @JoinColumn(name = "metadata_id"))
  @Column(name = "metadata")
  private List<MetaDataInfo> metaData = new ArrayList<MetaDataInfo>();

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "content_disposition")
  private String contentDisposition;

  @Column(name = "is_deleted")
  private Boolean deleted;

  @Column(name = "version_id")
  private String versionId;

  @Column(name = "is_last")
  private Boolean last;

  @Column(name = "upload_id")
  private String uploadId;

  @Column(name = "part_number")
  private Integer partNumber;

  @Column(name = "manifest")
  private Boolean manifest;

  @Column(name = "upload_complete")
  private Boolean uploadComplete;

  @Column(name = "cleanup")
  private Boolean cleanup;

  /**
   * Used to denote the object as a snapshot, for special access-control considerations.
   */
  @Column(name = "is_snapshot")
  private Boolean isSnapshot;

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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

  public Boolean getLast() {
    return last;
  }

  public void setLast(Boolean last) {
    this.last = last;
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

  public Boolean getManifest() {
    return manifest;
  }

  public void setManifest(Boolean manifest) {
    this.manifest = manifest;
  }

  public Boolean getUploadComplete() {
    return uploadComplete;
  }

  public void setUploadComplete(Boolean uploadComplete) {
    this.uploadComplete = uploadComplete;
  }

  public Boolean getCleanup() {
    return cleanup;
  }

  public void setCleanup(Boolean cleanup) {
    this.cleanup = cleanup;
  }

  public Boolean getIsSnapshot() {
    return isSnapshot;
  }

  public void setIsSnapshot(Boolean isSnapshot) {
    this.isSnapshot = isSnapshot;
  }

  public ObjectInfo() {}

  public ObjectInfo(String bucketName, String objectKey) {
    this.bucketName = bucketName;
    this.objectKey = objectKey;
  }

  public void replaceMetaData(List<MetaDataEntry> metaDataEntries) {
    metaData = new ArrayList<MetaDataInfo>();
    if (metaDataEntries != null) {
      for (MetaDataEntry metaDataEntry : metaDataEntries) {
        MetaDataInfo metaDataInfo = new MetaDataInfo();
        metaDataInfo.setObjectName(objectName);
        metaDataInfo.setName(metaDataEntry.getName());
        metaDataInfo.setValue(metaDataEntry.getValue());
        metaData.add(metaDataInfo);
      }
    }
  }

  public List<MetaDataInfo> cloneMetaData() {
    ArrayList<MetaDataInfo> metaDataInfos = new ArrayList<MetaDataInfo>();
    if (metaData != null) {
      for (MetaDataInfo metaDataInfo : metaData) {
        metaDataInfos.add(new MetaDataInfo(metaDataInfo));
      }
    }
    return metaDataInfos;
  }

  public boolean isMultipart() {
    return StringUtils.isNotBlank(uploadId);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bucketName == null) ? 0 : bucketName.hashCode());
    result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
    result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
    result = prime * result + ((partNumber == null) ? 0 : partNumber.hashCode());
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
    if (partNumber == null) {
      if (other.partNumber != null)
        return false;
    } else if (!partNumber.equals(other.partNumber))
      return false;
    return true;
  }
}
