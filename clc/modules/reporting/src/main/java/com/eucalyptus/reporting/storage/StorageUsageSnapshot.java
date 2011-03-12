package com.eucalyptus.reporting.storage;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Entity;

/**
 * <p>StorageUsageSnapshot is a snapshot of disk data usage at some point in
 * time.
 * 
 * @author twerges
 */
@Entity
@PersistenceContext(name="reporting")
@Table(name="storage_usage_snapshot")
class StorageUsageSnapshot
{
	@EmbeddedId
	protected SnapshotKey key;
	@Embedded
	protected StorageUsageData usageData;

	protected StorageUsageSnapshot()
	{
		
	}
	
	public StorageUsageSnapshot(SnapshotKey key, StorageUsageData usageData)
	{
		this.key = key;
		this.usageData = usageData;
	}

	public SnapshotKey getSnapshotKey()
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
