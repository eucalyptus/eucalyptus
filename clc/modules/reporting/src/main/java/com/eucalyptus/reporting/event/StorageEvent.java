package com.eucalyptus.reporting.event;

@SuppressWarnings("serial")
public class StorageEvent
	implements Event
{
	public enum EventType { S3Object, EbsVolume, EbsSnapshot };
	
	private final EventType eventType;
	private final boolean   createOrDelete;
	private final long      sizeGB;
	private final String    ownerId;
	private final String    accountId;
	private final String    clusterName;
	private final String    availabilityZone;
	
	public StorageEvent(EventType eventType, boolean createOrDelete,
			long sizeGB, String ownerId, String accountId, String clusterName,
			String availabilityZone)
	{
		this.eventType        = eventType;
		this.createOrDelete   = createOrDelete;
		this.sizeGB           = sizeGB;
		this.ownerId          = ownerId;
		this.accountId        = accountId;
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

	public long getSizeGB()
	{
		return sizeGB;
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

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}
	
}
