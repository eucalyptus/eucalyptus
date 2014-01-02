/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.entities;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;


//TODO: make this is a static class, not an entity, one config shared by all of the OSGs
@Entity
@PersistenceContext(name="eucalyptus_osg")
@Table( name = "osg_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "ObjectStorage", description = "Object Storage Gateway configuration.", deferred = true)
public class ObjectStorageGatewayInfo extends AbstractPersistent {
	@Column(name = "osg_name", unique=true)
	private String name;
	
	@ConfigurableField( description = "Maximum number of buckets per account", displayName = "Maximum buckets per account" )
	@Column( name = "storage_max_buckets_per_user" )
	private Integer storageMaxBucketsPerAccount;
	
	@ConfigurableField( description = "Maximum size per bucket", displayName = "Maximum bucket size (MB)" )
	@Column( name = "storage_max_bucket_size_mb" )
	private Integer storageMaxBucketSizeInMB;
	
	@ConfigurableField( description = "Image cache size", displayName = "Space reserved for unbundling images (MB)" )
	@Column( name = "storage_cache_size_mb" )
	private Integer storageMaxCacheSizeInMB;
	
	@ConfigurableField( description = "Disk space reserved for snapshots", displayName = "Space reserved for snapshots (GB)" )
	@Column( name = "storage_snapshot_size_gb" )
	private Integer storageMaxTotalSnapshotSizeInGb;
	
	@ConfigurableField( description = "Total ObjectStorage storage capacity for Objects", displayName = "ObjectStorage object capacity (GB)" )
	@Column( name = "storage_total_capacity" )
	private Integer storageMaxTotalCapacity;
	
	@ConfigurableField( description = "Number of hours to wait for object PUT operations to be allowed to complete before cleanup.", displayName = "Object PUT failure cleanup (Hours)", initial="24" )
	@Column( name = "failed_put_timeout_hours" )
	private Integer failedPutTimeoutHours;
	
	@ConfigurableField( description = "Interval, in seconds, at which cleanup tasks are initiated for removing old/stale objects.", displayName = "Cleanup interval (seconds)", initial="60")
	public Integer cleanupTaskIntervalSeconds;	
	
	public Integer getCleanupTaskIntervalSeconds() {
		return cleanupTaskIntervalSeconds;
	}

	public void setCleanupTaskIntervalSeconds(Integer cleanupTaskIntervalSeconds) {
		this.cleanupTaskIntervalSeconds = cleanupTaskIntervalSeconds;
	}

	public ObjectStorageGatewayInfo() {}

	public ObjectStorageGatewayInfo(final String name, 
			final Integer storageMaxBucketsPerAccount,
			final Integer storageMaxBucketSizeInMB,
			final Integer storageMaxCacheSizeInMB,
			final Integer storageMaxTotalSnapshotSizeInGb,
			final Integer storageMaxObjectCapacity)
	{
		this.name = name;
		this.storageMaxBucketsPerAccount = storageMaxBucketsPerAccount;
		this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
		this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
		this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
		this.storageMaxTotalCapacity = storageMaxObjectCapacity;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStorageMaxBucketsPerAccount() {
		return storageMaxBucketsPerAccount;
	}

	public void setStorageMaxBucketsPerAccount( final Integer storageMaxBucketsPerAccount ) {
		this.storageMaxBucketsPerAccount = storageMaxBucketsPerAccount;
	}

	public Integer getStorageMaxBucketSizeInMB() {
		return storageMaxBucketSizeInMB;
	}

	public void setStorageMaxBucketSizeInMB( final Integer storageMaxBucketSizeInMB ) {
		this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
	}

	public Integer getStorageMaxCacheSizeInMB() {
		return storageMaxCacheSizeInMB;
	}

	public void setStorageMaxCacheSizeInMB( final Integer storageMaxCacheSizeInMB ) {
		this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
	}

	public Integer getStorageMaxTotalSnapshotSizeInGb() {
		return storageMaxTotalSnapshotSizeInGb;
	}

	public void setStorageMaxTotalSnapshotSizeInGb( Integer storageMaxTotalSnapshotSizeInGb) {
		this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
	}

	public Integer getStorageMaxTotalCapacity() {
		return storageMaxTotalCapacity;
	}
	
	public void setStorageMaxTotalCapacity( final Integer storageMaxTotalCapacity) {
		this.storageMaxTotalCapacity = storageMaxTotalCapacity;
	}
	
	public Integer getFailedPutTimeoutHours() {
		return failedPutTimeoutHours;
	}

	public void setFailedPutTimeoutHours(Integer failedPutTimeoutHours) {
		this.failedPutTimeoutHours = failedPutTimeoutHours;
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
		ObjectStorageGatewayInfo other = (ObjectStorageGatewayInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	public static ObjectStorageGatewayInfo getObjectStorageGatewayInfo() {
		EntityWrapper<ObjectStorageGatewayInfo> db = EntityWrapper.get(ObjectStorageGatewayInfo.class);
		ObjectStorageGatewayInfo gatewayInfo = null;
		try {
			gatewayInfo = db.getUnique(new ObjectStorageGatewayInfo());
		} catch(Exception ex) {
			gatewayInfo = new ObjectStorageGatewayInfo(ObjectStorageProperties.NAME, 
					ObjectStorageProperties.MAX_BUCKETS_PER_ACCOUNT, 
					(int)(ObjectStorageProperties.MAX_BUCKET_SIZE / ObjectStorageProperties.M),
					(int)(ObjectStorageProperties.IMAGE_CACHE_SIZE / ObjectStorageProperties.M),
					ObjectStorageProperties.MAX_TOTAL_SNAPSHOT_SIZE, Integer.MAX_VALUE);
			gatewayInfo.setFailedPutTimeoutHours(ObjectStorageProperties.DEFAULT_PUT_TIMEOUT_HOURS);
			gatewayInfo.setCleanupTaskIntervalSeconds(ObjectStorageProperties.DEFAULT_CLEANUP_INTERVAL_SEC);
			db.add(gatewayInfo);
		} finally {
			db.commit();
		}
		return gatewayInfo;
	}
}
