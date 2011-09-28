package com.eucalyptus.reporting.modules.instance;

public class InstanceSummaryKey
{
	private final String ownerId;
	private final String accountId;
	private final String clusterName;
	private final String availabilityZone;

	public InstanceSummaryKey(String ownerId, String accountId,
			String clusterName, String availabilityZone)
	{
		super();
		this.ownerId = ownerId;
		this.accountId = accountId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
	}
	
	public InstanceSummaryKey(InstanceAttributes attrs)
	{
		this(attrs.getUserId(), attrs.getAccountId(), attrs
				.getClusterName(), attrs.getAvailabilityZone());
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
	
	public String toString()
	{
		return String.format("[owner:%s,account:%s,cluster:%s,zone:%s]",
				ownerId, accountId, clusterName, availabilityZone);
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
		InstanceSummaryKey other = (InstanceSummaryKey) obj;
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
		return true;
	}
	

}
