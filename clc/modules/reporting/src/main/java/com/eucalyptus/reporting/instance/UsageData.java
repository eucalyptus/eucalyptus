package com.eucalyptus.reporting.instance;

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
public class UsageData
{
	private final Long networkIoMegs;
	private final Long diskIoMegs;

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
	 * The numeric fields can be null, in which case the result will
	 * be null. If <i>either</i> operand is null, the result is null. Null
	 * plus anything is null.
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
	 * is null. If <i>either</i> operand is null, the result is null.
	 * Null minus anything is null.
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
