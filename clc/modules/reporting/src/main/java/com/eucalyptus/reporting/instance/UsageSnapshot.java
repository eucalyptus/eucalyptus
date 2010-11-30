package com.eucalyptus.reporting.instance;

import javax.persistence.*;

/**
 * Snapshot of the resource usage of some instance at some point in time.
 * Contains <i>cumulative</i> usage data so it's populated with
 * all resource usage which has occurred up until this snapshot was
 * instantiated. 
 * 
 * @author tom.werges
 */
@Embeddable
class UsageSnapshot
{
	@Column(name="timestamp_ms", nullable=false)
	private final Long timestampMs;
	@Embedded
	private final UsageData cumulativeUsageData;

	/**
	 * For hibernate usage only; don't extend this class
	 */
	protected UsageSnapshot()
	{
		this.timestampMs = null;  //hibernate will override these despite finality
		this.cumulativeUsageData = null;
	}

	public UsageSnapshot(long timestampMs, UsageData cumulativeUsageData)
	{
		this.timestampMs = new Long(timestampMs);
		this.cumulativeUsageData = cumulativeUsageData;
	}

	public long getTimestampMs()
	{
		assert this.timestampMs != null;  //hibernate notNullable
		return this.timestampMs.longValue();
	}

	public UsageData getCumulativeUsageData()
	{
		return this.cumulativeUsageData;
	}

}
