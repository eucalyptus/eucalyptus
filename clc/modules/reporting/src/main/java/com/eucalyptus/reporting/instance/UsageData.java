package com.eucalyptus.reporting.instance;

import java.io.Serializable;

import javax.persistence.*;

/**
 * <p>UsageData represents usage of resources (like disk bandwidth, etc)
 * by some instance over some period. UsageData is immutable because there
 * can be multiple references to one UsageData.
 * 
 * @author tom.werges
 */
@Embeddable
public class UsageData
	implements Serializable
{
	//NOTE: hibernate can modify final fields using reflection
	@Column(name="total_network_io_megs", nullable=false)
	private final Long networkIoMegs;
	@Column(name="total_disk_io_megs", nullable=false)
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

	public UsageData(long totalNetworkIoMegs, long totalDiskIoMegs)
	{
		this.networkIoMegs = new Long(totalNetworkIoMegs);
		this.diskIoMegs = new Long(totalDiskIoMegs);
	}

	public long getNetworkIoMegs()
	{
		assert this.networkIoMegs != null; //hibernate notNullable
		return this.networkIoMegs.longValue();
	}

	public long getDiskIoMegs()
	{
		assert this.diskIoMegs != null; //hibernate notNullable
		return this.diskIoMegs.longValue();
	}

	public UsageData sum(UsageData other)
	{
		long sumNetworkIoMegs = this.getNetworkIoMegs()
					+ other.getNetworkIoMegs();

		long sumDiskIoMegs = this.getDiskIoMegs()
					+ other.getDiskIoMegs();

		return new UsageData(sumNetworkIoMegs, sumDiskIoMegs);
	}

	public UsageData subtractFrom(UsageData other)
	{
		long subtractedNetworkIoMegs = other.getNetworkIoMegs()
					- this.getNetworkIoMegs();

		long subtractedDiskIoMegs = other.getDiskIoMegs()
					- this.getDiskIoMegs();

		return new UsageData(subtractedNetworkIoMegs, subtractedDiskIoMegs);
	}

}

