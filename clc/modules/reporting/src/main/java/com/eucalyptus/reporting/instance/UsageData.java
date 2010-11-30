package com.eucalyptus.reporting.instance;

import java.io.Serializable;

import javax.persistence.*;

/**
 * <p>UsageData represents usage of resources (like disk bandwidth, etc)
 * by some instance over some period. UsageData is immutable because there
 * can be multiple references to one UsageData.
 * 
 * <p>UsageData allows null values for its fields. Null values indicate unknown
 * usage and not zero usage.
 * 
 * @author tom.werges
 */
@Embeddable
public class UsageData
	implements Serializable
{
	//NOTE: hibernate can modify final fields using reflection
	@Column(name="total_network_io_megs", nullable=true)
	private final Long networkIoMegs;
	@Column(name="total_disk_io_megs", nullable=true)
	private final Long diskIoMegs;

	/**
	 * For hibernate usage only; don't extend this class
	 */
	protected UsageData()
	{
		//NOTE: hibernate will override these despite finality
		this.networkIoMegs = null;
		this.diskIoMegs = null;
	}

	public UsageData(Long totalNetworkIoMegs, Long totalDiskIoMegs)
	{
		this.networkIoMegs = totalNetworkIoMegs;
		this.diskIoMegs = totalDiskIoMegs;
	}

	public Long getNetworkIoMegs()
	{
		return this.networkIoMegs;
	}

	public Long getDiskIoMegs()
	{
		return this.diskIoMegs;
	}

	/**
	 * Sum usage data objects, meaning sum the numeric fields.
	 * The numeric fields can be null, in which case the result fields will
	 * be null. If <i>either</i> value for a field is null, the result is
	 * null, not the value. Null plus anything is null.
	 */
	public UsageData sum(UsageData other)
	{
		final Long sumNetworkIoMegs =
			(this.networkIoMegs==null || other.networkIoMegs==null)
			? null
			: new Long(other.networkIoMegs.longValue() + this.networkIoMegs.longValue());

		final Long sumDiskIoMegs =
			(this.diskIoMegs==null || other.diskIoMegs==null)
			? null
			: new Long(other.diskIoMegs.longValue() + this.diskIoMegs.longValue());

		return new UsageData(sumNetworkIoMegs, sumDiskIoMegs);
	}

	/**
	 * Subtract one usage data from another, meaning subtract the numeric
	 * fields. Fields can be null, in which case the resultant field
	 * is null. If <i>either</i> value of a field is null, the result
	 * is null. Null minus anything is null.
	 */
	public UsageData subtractFrom(UsageData other)
	{
		final Long subtractedNetworkIoMegs =
			(this.networkIoMegs==null || other.networkIoMegs==null)
			? null
			: new Long(other.networkIoMegs.longValue() - this.networkIoMegs.longValue());

		final Long subtractedDiskIoMegs =
			(this.diskIoMegs==null || other.diskIoMegs==null)
			? null
			: new Long(other.diskIoMegs.longValue() - this.diskIoMegs.longValue());

		return new UsageData(subtractedNetworkIoMegs, subtractedDiskIoMegs);
	}

}
