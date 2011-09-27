package com.eucalyptus.reporting.modules.storage;

import javax.persistence.*;

/**
 * <p>SnapshotKey is a key for looking up snapshots.
 * 
 * <p>It may seem excessive to have a database key with 5 key components, but
 * it's necessary. That's how the data is. There is a separate snapshot for each
 * {owner,account,cluster,zone,timestamp} tuple.
 * 
 * @author tom.werges
 */
@Embeddable
public class StorageSnapshotKey
	implements java.io.Serializable
{
	@Column(name="owner_id", nullable=false)
	protected final String ownerId;
	@Column(name="account_id", nullable=false)
	protected final String accountId;
	@Column(name="cluster_name", nullable=false)
	protected final String clusterName;
	@Column(name="availability_zone", nullable=false)
	protected final String availabilityZone;
	@Column(name="timestamp_ms", nullable=false)
	protected final Long timestampMs;
	
	protected StorageSnapshotKey()
	{
		this.ownerId = null;
		this.accountId = null;
		this.clusterName = null;
		this.availabilityZone = null;
		this.timestampMs = null;
	}

	public StorageSnapshotKey(String ownerId, String accountId, String clusterName,
			String availabilityZone, Long timestampMs)
	{
		this.ownerId = ownerId;
		this.accountId = accountId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
		this.timestampMs = timestampMs;
	}

	public String getOwnerId()
	{
		return ownerId;
	}

	public String getAccountId()
	{
		return accountId;
	}

	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public StorageSnapshotKey newKey(long newTimestampMs)
	{
		return new StorageSnapshotKey(ownerId, accountId, clusterName, availabilityZone,
				new Long(newTimestampMs));
	}
	
	@Override
	public String toString()
	{
		return String.format("[owner:%s,account:%s,cluster:%s,zone:%s,timestamp:%d]",
				ownerId, accountId, clusterName, availabilityZone, timestampMs);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accountId == null) ? 0 : accountId.hashCode());
		result = prime
				* result
				+ ((availabilityZone == null) ? 0 : availabilityZone.hashCode());
		result = prime * result
				+ ((clusterName == null) ? 0 : clusterName.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StorageSnapshotKey other = (StorageSnapshotKey) obj;
		if (accountId == null) {
			if (other.accountId != null)
				return false;
		} else if (!accountId.equals(other.accountId))
			return false;
		if (availabilityZone == null) {
			if (other.availabilityZone != null)
				return false;
		} else if (!availabilityZone.equals(other.availabilityZone))
			return false;
		if (clusterName == null) {
			if (other.clusterName != null)
				return false;
		} else if (!clusterName.equals(other.clusterName))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (timestampMs == null) {
			if (other.timestampMs != null)
				return false;
		} else if (!timestampMs.equals(other.timestampMs))
			return false;
		return true;
	}

}
