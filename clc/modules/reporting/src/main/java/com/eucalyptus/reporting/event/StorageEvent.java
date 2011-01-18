package com.eucalyptus.reporting.event;

@SuppressWarnings("serial")
public class StorageEvent
	implements java.io.Serializable
{
	public enum EventType { S3Object, EbsVolume, EbsSnapshot };
	
	private final EventType eventType;
	private final boolean   createOrDelete;
	private final long      sizeGb;  //gigaBYTES; java caps
	private final String    ownerId;
	private final String    accountId;
	private final String    clusterName;
	private final String    availabilityZone;
	private final long      timestampMs;
	
	public StorageEvent(EventType eventType, boolean createOrDelete,
			long sizeGb, String ownerId, String accountId, String clusterName,
			String availabilityZone, long timestampMs)
	{
		this.eventType        = eventType;
		this.createOrDelete   = createOrDelete;
		this.sizeGb           = sizeGb;
		this.ownerId          = ownerId;
		this.accountId        = accountId;
		this.clusterName      = clusterName;
		this.availabilityZone = availabilityZone;
		this.timestampMs      = timestampMs;
	}

	public EventType getEventType()
	{
		return eventType;
	}

	public boolean isCreateOrDelete()
	{
		return createOrDelete;
	}

	/**
	 * @return size in gigaBYTES; java caps
	 */
	public long getSizeGb()
	{
		return sizeGb;
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

	public long getTimestampMs()
	{
		return timestampMs;
	}
	
}
