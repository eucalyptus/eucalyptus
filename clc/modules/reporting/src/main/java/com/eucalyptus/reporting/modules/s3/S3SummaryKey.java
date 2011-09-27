package com.eucalyptus.reporting.modules.s3;


public class S3SummaryKey
{
	private final String ownerId;
	private final String accountId;

	public S3SummaryKey(String ownerId, String accountId)
	{
		super();
		this.ownerId = ownerId;
		this.accountId = accountId;
	}

	public S3SummaryKey(S3SnapshotKey snapshotKey)
	{
		this(snapshotKey.getOwnerId(), snapshotKey.getAccountId());
	}
	
	public String getOwnerId()
	{
		return ownerId;
	}

	public String getAccountId()
	{
		return accountId;
	}
	
	public String toString()
	{
		return String.format("[owner:%s,account:%s", ownerId, accountId);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accountId == null) ? 0 : accountId.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
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
		S3SummaryKey other = (S3SummaryKey) obj;
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
		return true;
	}

	
	
	
}
