package com.eucalyptus.reporting.modules.s3;

import javax.persistence.*;

@Embeddable
public class S3SnapshotKey
	implements java.io.Serializable
{
	@Column(name="owner_id", nullable=false)
	protected final String ownerId;
	@Column(name="account_id", nullable=false)
	protected final String accountId;
	@Column(name="timestamp_ms", nullable=false)
	protected final Long timestampMs;

	
	protected S3SnapshotKey()
	{
		this.ownerId = null;
		this.accountId = null;
		this.timestampMs = null;
	}

	/**
	 * Copy constructor to avoid Hibernate badness.
	 */
	public S3SnapshotKey(S3SnapshotKey key)
	{
		this(new String(key.getOwnerId()), new String(key.getAccountId()),
				new Long(key.getTimestampMs()));
	}
	
	public S3SnapshotKey(String ownerId, String accountId, Long timestampMs)
	{
		this.ownerId = ownerId;
		this.accountId = accountId;
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

	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public S3SnapshotKey newKey(long newTimestampMs)
	{
		return new S3SnapshotKey(ownerId, accountId, new Long(newTimestampMs));
	}
	
	@Override
	public String toString()
	{
		return String.format("[owner:%s,account:%s,timestamp:%d]", ownerId,
				accountId, timestampMs);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accountId == null) ? 0 : accountId.hashCode());
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
		S3SnapshotKey other = (S3SnapshotKey) obj;
		if (accountId == null) {
			if (other.accountId != null)
				return false;
		} else if (!accountId.equals(other.accountId))
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
