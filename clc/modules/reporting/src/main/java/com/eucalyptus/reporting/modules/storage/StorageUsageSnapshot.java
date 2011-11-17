package com.eucalyptus.reporting.modules.storage;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * <p>StorageUsageSnapshot is a snapshot of disk data usage at some point in
 * time.
 * 
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="storage_usage_snapshot")
public class StorageUsageSnapshot
	extends AbstractPersistent
{
	@Embedded
	protected StorageSnapshotKey key;
	@Embedded
	protected StorageUsageData usageData;
	@Column(name="is_all_snapshot", nullable=false)
	protected Boolean allSnapshot = false;
	
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
	
	public Boolean getAllSnapshot()
	{
		return allSnapshot;
	}

	public void setAllSnapshot(Boolean allSnapshot)
	{
		this.allSnapshot = allSnapshot;
	}
	
	public String toString()
	{
		return String.format("[key:%s,usageData:%s,allSnapshot:%b]",
				this.key, this.usageData, this.allSnapshot);
	}

	@Override
	public int hashCode()
	{
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		StorageUsageSnapshot other = (StorageUsageSnapshot) obj;
		return key.equals(other.key);
	}

}
