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
