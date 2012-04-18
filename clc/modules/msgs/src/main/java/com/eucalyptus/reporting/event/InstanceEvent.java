package com.eucalyptus.reporting.event;

/**
 * <p>InstanceEvent is an event sent from the CLC to the reporting mechanism,
 * indicating resource usage by an instance.
 * 
 * <p>InstanceEvent contains the <i>cumulative</i> usage of an instance up until
 * this point. For example, it contains all network and disk bandwidth used up
 * until the point when the InstanceEvent was instantiated. This is different
 * from data returned by the reporting mechanism, which contains only usage data
 * for a specific period.
 * 
 * <p>By using cumulative usage totals, we gain resilience to lost packets
 * despite unreliable transmission. If an event is lost, then the next event
 * will contain the cumulative usage and nothing will be lost, and the sampling
 * period will be assumed to have begun at the end of the last successfully
 * received event. As a result, lost packets cause only loss of granularity of
 * time periods, but no loss of usage information.
 * 
 * <p>InstanceEvent allows null values for usage statistics like
 * cumulativeDiskIo. Null values signify missing information and not zero
 * usage. Null values will be ignored while calculating aggregate usage
 * information for reports. Null values should be used only when we don't
 * support gathering information from an instance <i>at all</i>. Null values
 * for resource usage will be represented as "N/A" or something similar in
 * UIs.
 * 
 * @author tom.werges
 */
@SuppressWarnings("serial")
public class InstanceEvent
	implements Event
{
	private String uuid;
	private String instanceId;
	private String instanceType;
	private String userId;
	private String userName;
	private String accountId;
	private String accountName;
	private String clusterName;
	private String availabilityZone;
	private Long   cumulativeNetworkIoMegs;
	private Long   cumulativeDiskIoMegs;
	

	public InstanceEvent(String uuid, String instanceId, String instanceType,
			String userId, String userName, String accountId,
			String accountName, String clusterName,	String availabilityZone,
			Long cumulativeNetworkIoMegs, Long cumulativeDiskIoMegs)
	{
		this.uuid = uuid;
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
		this.userName = userName;
		this.accountId = accountId;
		this.accountName = accountName;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
		this.cumulativeNetworkIoMegs = cumulativeNetworkIoMegs;
		this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;
	}

	public String getUuid()
	{
		return uuid;
	}

	public String getInstanceId()
	{
		return instanceId;
	}

	public String getInstanceType()
	{
		return instanceType;
	}

	public String getUserId()
	{
		return userId;
	}
	
	public String getUserName()
	{
		return userName;
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

	public Long getCumulativeNetworkIoMegs()
	{
		return cumulativeNetworkIoMegs;
	}

	public Long getCumulativeDiskIoMegs()
	{
		return cumulativeDiskIoMegs;
	}

	public boolean requiresReliableTransmission()
	{
		return false;
	}
	
	public String toString()
	{
		return String.format("[uuid:%s,instanceId:%s,instanceType:%s,userId:%s"
				+ ",accountId:%s,cluster:%s,zone:%s,net:%d,disk:%d]",
					uuid, instanceId, instanceType, userId, accountId,
					clusterName, availabilityZone, cumulativeNetworkIoMegs,
					cumulativeDiskIoMegs);
	}

}
