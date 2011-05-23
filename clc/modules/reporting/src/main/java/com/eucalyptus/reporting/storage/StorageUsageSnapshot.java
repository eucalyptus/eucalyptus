package com.eucalyptus.reporting.storage;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

/**
 * <p>StorageUsageSnapshot is a snapshot of disk data usage at some point in
 * time.
 * 
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="reporting")
@Table(name="storage_usage_snapshot")
class StorageUsageSnapshot
{
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	private Long id;
	@Embedded
	protected StorageSnapshotKey key;
	@Embedded
	protected StorageUsageData usageData;
	
	protected StorageUsageSnapshot()
	{
		
	}

	public StorageUsageSnapshot(StorageSnapshotKey key, StorageUsageData usageData)
	{
		this.key = key;
		this.usageData = usageData;
	}

	public StorageSnapshotKey getSnapshotKey()
	{
		return key;
	}

	public StorageUsageData getUsageData()
	{
		return usageData;
	}

	public String toString()
	{
		return String.format("[key:%s,usageData:%s]", this.key, this.usageData);
	}

}
