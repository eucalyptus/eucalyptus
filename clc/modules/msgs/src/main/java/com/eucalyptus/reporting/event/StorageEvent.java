package com.eucalyptus.reporting.event;

@SuppressWarnings("serial")
public class StorageEvent
	implements Event
{
	public enum EventType { EbsVolume, EbsSnapshot };
	
	private final EventType eventType;
	private final boolean   createOrDelete;
	private final long      sizeMegs;
	private final String    ownerId;
	private final String    ownerName;
	private final String    accountId;
	private final String    accountName;
	private final String    clusterName;
	private final String    availabilityZone;
	
	public StorageEvent(EventType eventType, boolean createOrDelete,
			long sizeMegs, String ownerId, String ownerName, String accountId,
			String accountName, String clusterName,	String availabilityZone)
	{
		if (eventType==null) throw new IllegalArgumentException("eventType cant be null");
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (ownerName==null) throw new IllegalArgumentException("ownerName cant be null");
		if (accountId==null) throw new IllegalArgumentException("accountId cant be null");
		if (accountName==null) throw new IllegalArgumentException("accountName cant be null");
		if (clusterName==null) throw new IllegalArgumentException("clusterName cant be null");
		if (availabilityZone==null) throw new IllegalArgumentException("availabilityZone cant be null");
		if (sizeMegs < 0) throw new IllegalArgumentException("Storage size cannot be negative");

		this.eventType        = eventType;
		this.createOrDelete   = createOrDelete;
		this.sizeMegs         = sizeMegs;
		this.ownerId          = ownerId;
		this.ownerName        = ownerName;
		this.accountId        = accountId;
		this.accountName      = accountName;
		this.clusterName      = clusterName;
		this.availabilityZone = availabilityZone;
	}

	public EventType getEventType()
	{
		return eventType;
	}

	public boolean isCreateOrDelete()
	{
		return createOrDelete;
	}

	public long getSizeMegs()
	{
		return sizeMegs;
	}

	public String getOwnerId()
	{
		return ownerId;
	}
	
	public String getOwnerName()
	{
		return ownerName;
	}

	public String getAccountId()
	{
		return accountId;
	}
	
	public String getAccountName()
	{
		return accountName;
	}

	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}
	
}
