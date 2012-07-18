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
 ************************************************************************/

package edu.ucsb.eucalyptus.cloud.entities;

import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "walrus_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "walrus", description = "Walrus configuration.", deferred = true)
public class WalrusInfo extends AbstractPersistent {
	@Column(name = "walrus_name", unique=true)
	private String name;
	@ConfigurableField( description = "Path to buckets storage", displayName = "Buckets Path" )
	@Column( name = "storage_dir" )
	private String storageDir;
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

	public WalrusInfo() {}

	public WalrusInfo(final String name, 
			final String storageDir,
			final Integer storageMaxBucketsPerAccount,
			final Integer storageMaxBucketSizeInMB,
			final Integer storageMaxCacheSizeInMB,
			final Integer storageMaxTotalSnapshotSizeInGb)
	{
		this.name = name;
		this.storageDir = storageDir;
		this.storageMaxBucketsPerAccount = storageMaxBucketsPerAccount;
		this.storageMaxBucketSizeInMB = storageMaxBucketSizeInMB;
		this.storageMaxCacheSizeInMB = storageMaxCacheSizeInMB;
		this.storageMaxTotalSnapshotSizeInGb = storageMaxTotalSnapshotSizeInGb;
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
		EntityWrapper<WalrusInfo> db = EntityWrapper.get(WalrusInfo.class);
		WalrusInfo walrusInfo;
		try {
			walrusInfo = db.getUnique(new WalrusInfo());
		} catch(Exception ex) {
			walrusInfo = new WalrusInfo(WalrusProperties.NAME, 
					WalrusProperties.bucketRootDirectory, 
					WalrusProperties.MAX_BUCKETS_PER_ACCOUNT, 
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
