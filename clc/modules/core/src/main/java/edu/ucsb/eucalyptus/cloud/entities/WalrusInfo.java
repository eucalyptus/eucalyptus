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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

import javax.persistence.*;

@Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "walrus_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
@ConfigurableClass(root = "walrus", description = "Walrus configuration.", deferred = true)
public class WalrusInfo {
	@Id
	@GeneratedValue
	@Column( name = "walrus_info_id" )
	private Long id = -1l;
	@Column(name = "walrus_name", unique=true)
	private String name;
	@ConfigurableField( description = "Path to buckets storage", displayName = "Buckets Path" )
	@Column( name = "storage_dir" )
	private String storageDir;
	@ConfigurableField( description = "Maximum number of buckets per user", displayName = "Maximum buckets per user" )
	@Column( name = "storage_max_buckets_per_user" )
	private Integer storageMaxBucketsPerUser;
	@ConfigurableField( description = "Maximum size per bucket", displayName = "Maximum bucket size (MB)" )
	@Column( name = "storage_max_bucket_size_mb" )
	private Integer storageMaxBucketSizeInMB;
	@ConfigurableField( description = "Image cache size", displayName = "Space reserved for unbundling images (MB)" )
	@Column( name = "storage_cache_size_mb" )
	private Integer storageMaxCacheSizeInMB;
	@ConfigurableField( description = "Disk space reserved for snapshots", displayName = "Space reserved for snapshots (GB)" )
	@Column( name = "storage_snapshot_size_gb" )
	private Integer storageMaxTotalSnapshotSizeInGb;

	public WalrusInfo() {}

	public WalrusInfo(final String name, 
			final String storageDir,
			final Integer storageMaxBucketsPerUser,
			final Integer storageMaxBucketSizeInMB,
			final Integer storageMaxCacheSizeInMB,
			final Integer storageMaxTotalSnapshotSizeInGb)
	{
		this.name = name;
		this.storageDir = storageDir;
		this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
		this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
		this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
		this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
	}

	public Long getId() {
		return id;
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

	public void setStorageDir( final String storageDir ) {
		this.storageDir = storageDir;
	}

	public Integer getStorageMaxBucketsPerUser() {
		return storageMaxBucketsPerUser;
	}

	public void setStorageMaxBucketsPerUser( final Integer storageMaxBucketsPerUser ) {
		this.storageMaxBucketsPerUser = storageMaxBucketsPerUser;
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

	public static WalrusInfo getWalrusInfo() {
		EntityWrapper<WalrusInfo> db = new EntityWrapper<WalrusInfo>(WalrusProperties.DB_NAME);
		WalrusInfo walrusInfo;
		try {
			walrusInfo = db.getUnique(new WalrusInfo());
		} catch(EucalyptusCloudException ex) {
			walrusInfo = new WalrusInfo(WalrusProperties.NAME, 
					WalrusProperties.bucketRootDirectory, 
					WalrusProperties.MAX_BUCKETS_PER_USER, 
					(int)(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M),
					(int)(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties.M),
					WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE);
			db.add(walrusInfo);     
		} finally {
			db.commit();
		}
		return walrusInfo;
	}
}
