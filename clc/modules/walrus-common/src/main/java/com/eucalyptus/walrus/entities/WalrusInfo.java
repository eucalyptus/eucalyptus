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

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.walrus.util.WalrusProperties;

@Entity
@PersistenceContext(name = "eucalyptus_walrus")
@Table(name = "walrus_info")
@ConfigurableClass(root = "walrusbackend", description = "WalrusBackend backend configuration.", deferred = true)
public class WalrusInfo extends AbstractPersistent {
  @Column(name = "walrus_name", unique = true)
  private String name;
  @ConfigurableField(description = "Path to buckets storage", displayName = "Buckets Path")
  @Column(name = "storage_dir")
  private String storageDir;

  @Column(name = "storage_max_buckets_per_user")
  private Integer storageMaxBucketsPerAccount;

  @Column(name = "storage_max_bucket_size_mb")
  private Integer storageMaxBucketSizeInMB;

  @Column(name = "storage_snapshot_size_gb")
  private Integer storageMaxTotalSnapshotSizeInGb;

  @ConfigurableField(description = "Total WalrusBackend storage capacity for Objects", displayName = "WalrusBackend object capacity (GB)")
  @Column(name = "storage_walrus_total_capacity")
  private Integer storageMaxTotalCapacity;

  @Column(name = "storage_walrus_bucket_names_require_compliance")
  private Boolean bucketNamesRequireDnsCompliance;

  private static final Logger LOG = Logger.getLogger(WalrusInfo.class);

  public WalrusInfo() {}

  public WalrusInfo(final String name, final String storageDir, final Integer storageMaxBucketsPerAccount, final Integer storageMaxBucketSizeInMB,
      final Integer storageMaxTotalSnapshotSizeInGb, final Integer storageMaxObjectCapacity, final Boolean bucketNamesRequireDnsCompliance) {
    this.name = name;
    this.storageDir = storageDir;
    this.storageMaxBucketsPerAccount = storageMaxBucketsPerAccount;
    this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
    this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
    this.storageMaxTotalCapacity = storageMaxObjectCapacity;
    this.bucketNamesRequireDnsCompliance = bucketNamesRequireDnsCompliance;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStorageDir() {
    return storageDir;
  }

  public void setStorageDir(final String storageDir) {
    this.storageDir = storageDir;
  }

  public Integer getStorageMaxBucketsPerAccount() {
    return storageMaxBucketsPerAccount;
  }

  public void setStorageMaxBucketsPerAccount(final Integer storageMaxBucketsPerAccount) {
    this.storageMaxBucketsPerAccount = storageMaxBucketsPerAccount;
  }

  public Integer getStorageMaxBucketSizeInMB() {
    return storageMaxBucketSizeInMB;
  }

  public void setStorageMaxBucketSizeInMB(final Integer storageMaxBucketSizeInMB) {
    this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
  }

  public Integer getStorageMaxTotalSnapshotSizeInGb() {
    return storageMaxTotalSnapshotSizeInGb;
  }

  public void setStorageMaxTotalSnapshotSizeInGb(Integer storageMaxTotalSnapshotSizeInGb) {
    this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
  }

  public Integer getStorageMaxTotalCapacity() {
    return storageMaxTotalCapacity;
  }

  public void setStorageMaxTotalCapacity(final Integer storageMaxTotalCapacity) {
    this.storageMaxTotalCapacity = storageMaxTotalCapacity;
  }

  public Boolean getBucketNamesRequireDnsCompliance() {
    return bucketNamesRequireDnsCompliance;
  }

  public void setBucketNamesRequireDnsCompliance(Boolean bucketNamesRequireDnsCompliance) {
    this.bucketNamesRequireDnsCompliance = bucketNamesRequireDnsCompliance;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    WalrusInfo other = (WalrusInfo) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  private static int estimateWalrusCapacity() {
    // Load the defaults.
    // Try to determine available space on the bucket root directory.
    int capacity = WalrusProperties.DEFAULT_INITIAL_CAPACITY;
    try {
      long bytesAvailable = new File(WalrusProperties.bucketRootDirectory).getUsableSpace(); // keep 1GB at least reserved.

      // Set initial capacity to available space minus 1GB unless there is less than 1GB avaiable.
      // zhill: The cast to int should only affect systems with more than 2^31-1 GB capacity (2.1 Exabytes), so we should be safe for a while
      capacity = (int) (bytesAvailable / WalrusProperties.G);
      capacity = (capacity > 1 ? capacity - 1 : capacity);

    } catch (Exception e) {
      LOG.warn("Unable to detect usable space in the directory:" + WalrusProperties.bucketRootDirectory + " because of exception: " + e.getMessage()
          + ". Using WalrusBackend default: " + WalrusProperties.DEFAULT_INITIAL_CAPACITY + "GB");
    }
    return capacity;
  }

  @PreUpdate
  public void preUpdateChecks() {
    // cover the upgrade case
    if (this.getStorageMaxTotalCapacity() == null) {
      this.setStorageMaxTotalCapacity(estimateWalrusCapacity());
    }
    if (this.getBucketNamesRequireDnsCompliance() == null) {
      this.setBucketNamesRequireDnsCompliance(new Boolean(WalrusProperties.BUCKET_NAMES_REQUIRE_DNS_COMPLIANCE));
    }
  }

  private static WalrusInfo generateDefault() {
    return new WalrusInfo(WalrusProperties.NAME, WalrusProperties.bucketRootDirectory, WalrusProperties.MAX_BUCKETS_PER_ACCOUNT,
        (int) (WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M), WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE, estimateWalrusCapacity(),
        new Boolean(WalrusProperties.BUCKET_NAMES_REQUIRE_DNS_COMPLIANCE));
  }

  public static WalrusInfo getWalrusInfo() {
    WalrusInfo walrusInfo = null;
    try {
      walrusInfo = Transactions.find(new WalrusInfo());
    } catch (Exception e) {
      LOG.warn("Walrus configuration not found. Loading defaults.");
      try {
        walrusInfo = Transactions.saveDirect(generateDefault());
      } catch (Exception e1) {
        try {
          walrusInfo = Transactions.find(new WalrusInfo());
        } catch (Exception e2) {
          LOG.warn("Failed to persist and retrieve WalrusInfo entity");
        }
      }
    }

    if (walrusInfo == null) {
      walrusInfo = generateDefault();
    }

    return walrusInfo;
  }
}
