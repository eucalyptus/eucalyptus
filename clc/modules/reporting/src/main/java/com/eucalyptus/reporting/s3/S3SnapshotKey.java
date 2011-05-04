package com.eucalyptus.reporting.s3;

import javax.persistence.*;

/**
 * <p>S3SnapshotKey is a key for looking up snapshots.
 * 
 * @author tom.werges
 */
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

}
