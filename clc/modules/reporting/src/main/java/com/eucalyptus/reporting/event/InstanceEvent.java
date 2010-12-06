package com.eucalyptus.reporting.event;

import javax.persistence.Column;

import com.eucalyptus.reporting.instance.InstanceAttributes;
import com.eucalyptus.reporting.instance.UsageData;

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
 * @author tom.werges
 */
public class InstanceEvent
	implements Event
{
	private String uuid;
	private String instanceId;
	private String instanceType;
	private String userId;
	private String clusterName;
	private String availabilityZone;
	private long cumulativeNetworkIoMegs;
	private long cumulativeDiskIoMegs;
	

	public InstanceEvent(String uuid, String instanceId, String instanceType,
			String userId, String clusterName, String availabilityZone,
			long cumulativeNetworkIoMegs, long cumulativeDiskIoMegs)
	{
		this.uuid = uuid;
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
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

	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	public long getCumulativeNetworkIoMegs()
	{
		return cumulativeNetworkIoMegs;
	}

	public long getCumulativeDiskIoMegs()
	{
		return cumulativeDiskIoMegs;
	}

	public boolean requiresReliableTransmission()
	{
		return false;
	}

}
