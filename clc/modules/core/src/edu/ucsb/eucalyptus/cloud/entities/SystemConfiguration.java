/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 * Author: Sunil Soman sunils@cs.ucsb.edu
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "system_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SystemConfiguration {
  @Id
  @GeneratedValue
  @Column( name = "system_info_id" )
  private Long id = -1l;
  @Column( name = "system_info_storage_url" )
  private String storageUrl;
  @Column( name = "system_info_default_kernel" )
  private String defaultKernel;
  @Column( name = "system_info_default_ramdisk" )
  private String defaultRamdisk;
  @Column( name = "system_storage_dir" )
  private String storageDir;
  @Column( name = "system_storage_max_buckets_per_user" )
  private Integer storageMaxBucketsPerUser;
  @Column( name = "system_storage_max_bucket_size_mb" )
  private Integer storageMaxBucketSizeInMB;
  @Column( name = "system_storage_cache_size_mb" )
  private Integer storageMaxCacheSizeInMB;
  @Column( name = "system_storage_volume_size_gb" )
  private Integer storageMaxTotalVolumeSizeInGb;
  @Column( name = "system_storage_snapshot_size_gb" )
  private Integer storageMaxTotalSnapshotSizeInGb;
  @Column( name = "system_registration_id" )
  private String registrationId;
  @Column( name = "system_storage_max_volume_size_gb")
  private Integer storageMaxVolumeSizeInGB;
  @Column( name = "system_storage_volumes_dir" )
  private String storageVolumesDir;

    public SystemConfiguration() {}

  public SystemConfiguration(final String storageUrl,
	final String defaultKernel,
	final String defaultRamdisk,
	final String storageDir,
	final Integer storageMaxBucketsPerUser,
	final Integer storageMaxBucketSizeInMB,
	final Integer storageMaxCacheSizeInMB,
	final Integer storageMaxTotalVolumeSizeInGb,
	final Integer storageMaxTotalSnapshotSizeInGb,
	final Integer storageMaxVolumeSizeInGB,
	final String storageVolumesDir)
  {
    this.storageUrl = storageUrl;
    this.defaultKernel = defaultKernel;
    this.defaultRamdisk = defaultRamdisk;
    this.storageDir = storageDir;
    this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
    this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
    this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
	this.storageMaxTotalVolumeSizeInGb = storageMaxTotalVolumeSizeInGb;
	this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
	this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
	this.storageVolumesDir = storageVolumesDir;
  }

  public Long getId() {
    return id;
  }

  public String getStorageUrl() {
    return storageUrl;
  }

  public String getDefaultKernel() {
    return defaultKernel;
  }

  public String getDefaultRamdisk() {
    return defaultRamdisk;
  }

  public void setStorageUrl( final String storageUrl ) {
    this.storageUrl = storageUrl;
  }

  public void setDefaultKernel( final String defaultKernel ) {
    this.defaultKernel = defaultKernel;
  }

  public void setDefaultRamdisk( final String defaultRamdisk ) {
    this.defaultRamdisk = defaultRamdisk;
  }

  public String getStorageDir() {
    return storageDir;
  }

  public void setStorageDir( final String storageDir ) {
    this.storageDir = storageDir;
  }

  public Integer getStorageMaxBucketsPerUser() {
    return storageMaxBucketsPerUser;
  }

  public void setStorageMaxBucketsPerUser( final Integer storageMaxBucketsPerUser ) {
    this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
  }

  public int getStorageMaxBucketSizeInMB() {
    return storageMaxBucketSizeInMB;
  }

  public void setStorageMaxBucketSizeInMB( final Integer storageMaxBucketSizeInMB ) {
    this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
  }

  public Integer getStorageMaxCacheSizeInMB() {
    return storageMaxCacheSizeInMB;
  }

  public void setStorageMaxCacheSizeInMB( Integer storageMaxCacheSizeInMB ) {
    this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
  }

  public Integer getStorageMaxTotalVolumeSizeInGb() {
    return storageMaxTotalVolumeSizeInGb;
  }

  public void setStorageMaxTotalVolumeSizeInGb( Integer storageMaxTotalVolumeSizeInGb ) {
    this.storageMaxTotalVolumeSizeInGb = storageMaxTotalVolumeSizeInGb;
  }

  public Integer getStorageMaxTotalSnapshotSizeInGb() {
    return storageMaxTotalSnapshotSizeInGb;
  }

  public void setStorageMaxTotalSnapshotSizeInGb( Integer storageMaxTotalSnapshotSizeInGb) {
    this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
  }

  public String getRegistrationId() {
    return registrationId;
  }

  public void setRegistrationId( final String registrationId ) {
    this.registrationId = registrationId;
  }

  public Integer getStorageMaxVolumeSizeInGB() {
    return storageMaxVolumeSizeInGB;
  }

  public void setStorageMaxVolumeSizeInGB( Integer storageMaxVolumeSizeInGB ) {
    this.storageMaxVolumeSizeInGB = storageMaxVolumeSizeInGB;
  }

  public String getStorageVolumesDir() {
    return storageVolumesDir;
  }

  public void setStorageVolumesDir( final String storageVolumesDir ) {
    this.storageVolumesDir = storageVolumesDir;
  }
}
