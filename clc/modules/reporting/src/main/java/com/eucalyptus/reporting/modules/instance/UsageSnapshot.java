package com.eucalyptus.reporting.modules.instance;

/**
 * UsageSnapshot of the resource usage of some instance at some point in time.
 * Contains <i>cumulative</i> usage data so it's populated with
 * all resource usage which has occurred up until this snapshot was
 * instantiated. 
 * 
 * @author tom.werges
 */
class UsageSnapshot
{
	private final Long timestampMs;
	private final InstanceUsageData cumulativeUsageData;

	/**
	 * For hibernate usage only; don't extend this class
	 */
	protected UsageSnapshot()
	{
		this.timestampMs = null;
		this.cumulativeUsageData = null;
	}

	public UsageSnapshot(long timestampMs, InstanceUsageData cumulativeUsageData)
	{
		this.timestampMs = new Long(timestampMs);
		this.cumulativeUsageData = cumulativeUsageData;
	}

	public long getTimestampMs()
	{
		assert this.timestampMs != null;
		return this.timestampMs.longValue();
	}

	public InstanceUsageData getCumulativeUsageData()
	{
		return this.cumulativeUsageData;
	}

}
