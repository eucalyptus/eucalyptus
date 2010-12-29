package com.eucalyptus.reporting.instance;

/**
 * <p>UsageData represents usage of resources (like disk bandwidth, etc)
 * by some instance over some period. UsageData is immutable, because there
 * can be multiple references to one UsageData.
 * 
 * <p>Null values for a field indicate unknown usage, not zero usage.
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
	 * Sum the numeric fields of two UsageData objects. If either operand is null
	 * for any field then the resultant field is null.
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
	 * Subtract the numeric fields of one UsageData from another. If either 
	 * operand is null for any field then the resultant field is null.
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
	
	
	/**
	 * toString() for debugging and logs
	 */
	public String toString()
	{
		return String.format("[disk:%d,net:%d]", this.diskIoMegs, this.networkIoMegs);
	}

}
