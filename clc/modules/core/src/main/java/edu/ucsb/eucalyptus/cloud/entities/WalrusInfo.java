package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "walrus_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class WalrusInfo {
	@Id
	@GeneratedValue
	@Column( name = "walrus_info_id" )
	private Long id = -1l;
	@Column(name = "walrus_name")
	private String name;
	@Column( name = "storage_dir" )
	private String storageDir;
	@Column( name = "storage_max_buckets_per_user" )
	private Integer storageMaxBucketsPerUser;
	@Column( name = "storage_max_bucket_size_mb" )
	private Integer storageMaxBucketSizeInMB;
	@Column( name = "storage_cache_size_mb" )
	private Integer storageMaxCacheSizeInMB;
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
}
