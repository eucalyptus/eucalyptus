package com.eucalyptus.reporting.instance;

import com.eucalyptus.reporting.Period;

/**
 * <p>UsageSummary is a summary of all resource usage for some entity over a
 * single time period. An entity can be a user, a group, an account, an
 * availability zone, a cluster, and so on. UsageSummary includes instance
 * types, instance running times, and resource usage. UsageSummary includes
 * everything in a single bean which is necessary to generate a jasper
 * report of instance usage.
 * 
 * @author tom.werges
 */
public class InstanceUsageSummary
{
	private Long m1SmallNum;
	private Long m1SmallTimeSecs;
	private Long c1MediumNum;
	private Long c1MediumTimeSecs;
	private Long m1LargeNum;
	private Long m1LargeTimeSecs;
	private Long m1XLargeNum;
	private Long m1XLargeTimeSecs;
	private Long c1XLargeNum;
	private Long c1XLargeTimeSecs;
	private Long networkIoMegs;
	private Long diskIoMegs;
	
	public InstanceUsageSummary()
	{
		this.m1SmallNum = new Long(0);
		this.m1SmallTimeSecs = new Long(0);
		this.c1MediumNum = new Long(0);
		this.c1MediumTimeSecs = new Long(0);
		this.m1LargeNum = new Long(0);
		this.m1LargeTimeSecs = new Long(0);
		this.m1XLargeNum = new Long(0);
		this.m1XLargeTimeSecs = new Long(0);
		this.c1XLargeNum = new Long(0);
		this.c1XLargeTimeSecs = new Long(0);
		this.networkIoMegs = new Long(0);
		this.diskIoMegs = new Long(0);
	}

	public InstanceUsageSummary(Long m1SmallNum, Long m1SmallTimeSecs,
			Long c1MediumNum, Long c1MediumTimeSecs, Long m1LargeNum,
			Long m1LargeTimeSecs, Long m1XLargeNum, Long m1XLargeTimeSecs,
			Long c1XLargeNum, Long c1XLargeTimeSecs, Long networkIoMegs,
			Long diskIoMegs)
	{
		//Check that no args are null
		if (m1SmallNum == null || m1SmallTimeSecs == null || c1MediumNum == null
			|| c1MediumTimeSecs == null || m1LargeNum == null || m1LargeTimeSecs == null
			|| m1XLargeNum == null || m1XLargeTimeSecs == null || c1XLargeNum == null
			|| c1XLargeTimeSecs == null || networkIoMegs == null || diskIoMegs == null)
		{
			throw new IllegalArgumentException("ctor args cannot be null");
		}
		
		this.m1SmallNum = m1SmallNum;
		this.m1SmallTimeSecs = m1SmallTimeSecs;
		this.c1MediumNum = c1MediumNum;
		this.c1MediumTimeSecs = c1MediumTimeSecs;
		this.m1LargeNum = m1LargeNum;
		this.m1LargeTimeSecs = m1LargeTimeSecs;
		this.m1XLargeNum = m1XLargeNum;
		this.m1XLargeTimeSecs = m1XLargeTimeSecs;
		this.c1XLargeNum = c1XLargeNum;
		this.c1XLargeTimeSecs = c1XLargeTimeSecs;
		this.networkIoMegs = networkIoMegs;
		this.diskIoMegs = diskIoMegs;
	}

	public Long getM1SmallNum()
	{
		return m1SmallNum;
	}

	public void setM1SmallNum(Long m1SmallNum)
	{
		if (m1SmallNum == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1SmallNum = m1SmallNum;
	}

	public Long getM1SmallTimeSecs()
	{
		return m1SmallTimeSecs;
	}

	public void setM1SmallTimeSecs(Long m1SmallTimeSecs)
	{
		if (m1SmallTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1SmallTimeSecs = m1SmallTimeSecs;
	}

	public Long getC1MediumNum()
	{
		return c1MediumNum;
	}

	public void setC1MediumNum(Long c1MediumNum)
	{
		if (c1MediumNum == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1MediumNum = c1MediumNum;
	}

	public Long getC1MediumTimeSecs()
	{
		return c1MediumTimeSecs;
	}

	public void setC1MediumTimeSecs(Long c1MediumTimeSecs)
	{
		if (c1MediumTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1MediumTimeSecs = c1MediumTimeSecs;
	}

	public Long getM1LargeNum()
	{
		return m1LargeNum;
	}

	public void setM1LargeNum(Long m1LargeNum)
	{
		if (m1LargeNum  == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1LargeNum = m1LargeNum;
	}

	public Long getM1LargeTimeSecs()
	{
		return m1LargeTimeSecs;
	}

	public void setM1LargeTimeSecs(Long m1LargeTimeSecs)
	{
		if (m1LargeTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1LargeTimeSecs = m1LargeTimeSecs;
	}

	public Long getM1XLargeNum()
	{
		return m1XLargeNum;
	}

	public void setM1XLargeNum(Long m1XLargeNum)
	{
		if (m1XLargeNum == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1XLargeNum = m1XLargeNum;
	}

	public Long getM1XLargeTimeSecs()
	{
		return m1XLargeTimeSecs;
	}

	public void setM1XLargeTimeSecs(Long m1XLargeTimeSecs)
	{
		if (m1XLargeTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1XLargeTimeSecs = m1XLargeTimeSecs;
	}

	public Long getC1XLargeNum()
	{
		return c1XLargeNum;
	}

	public void setC1XLargeNum(Long c1XLargeNum)
	{
		if (c1XLargeNum == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1XLargeNum = c1XLargeNum;
	}

	public Long getC1XLargeTimeSecs()
	{
		return c1XLargeTimeSecs;
	}

	public void setC1XLargeTimeSecs(Long c1XLargeTimeSecs)
	{
		if (c1XLargeTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1XLargeTimeSecs = c1XLargeTimeSecs;
	}

	public Long getNetworkIoMegs()
	{
		return networkIoMegs;
	}

	public void setNetworkIoMegs(Long networkIoMegs)
	{
		if (networkIoMegs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.networkIoMegs = networkIoMegs;
	}

	public Long getDiskIoMegs()
	{
		return diskIoMegs;
	}

	public void setDiskIoMegs(Long diskIoMegs)
	{
		if (diskIoMegs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.diskIoMegs = diskIoMegs;
	}

	public void sumFromUsageData(UsageData ud)
	{
		//Autoboxing should work because I prevented nulls everywhere
		this.diskIoMegs = this.diskIoMegs + ud.getDiskIoMegs();
		this.networkIoMegs = this.networkIoMegs + ud.getNetworkIoMegs();
	}
	
	public void sumFromPeriodType(Period period, String type)
	{
		if (period == null || type == null) {
			throw new IllegalArgumentException("args can't be null");
		}
		long timeSecs = (period.getEndingMs() - period.getBeginningMs())/1000;

		//Autoboxing should work because we prevented nulls everywhere
		if (type.equalsIgnoreCase("m1small")) {
			this.m1SmallNum = this.m1SmallNum + 1;
			this.m1SmallTimeSecs = this.m1SmallTimeSecs + timeSecs;
		} else if (type.equalsIgnoreCase("c1medium")) {
			this.c1MediumNum = this.c1MediumNum + 1;
			this.c1MediumTimeSecs = this.c1MediumTimeSecs + timeSecs;
		} else if (type.equalsIgnoreCase("m1large")) {
			this.m1LargeNum = this.m1LargeNum + 1;
			this.m1LargeTimeSecs = this.m1LargeTimeSecs + timeSecs;
		} else if (type.equalsIgnoreCase("m1xlarge")) {
			this.m1XLargeNum = this.m1XLargeNum + 1;
			this.m1XLargeTimeSecs = this.m1XLargeTimeSecs + timeSecs;
		} else if (type.equalsIgnoreCase("c1xlarge")) {
			this.c1XLargeNum = this.c1XLargeNum + 1;
			this.c1XLargeTimeSecs = this.c1XLargeTimeSecs + timeSecs;
		} else {
			System.err.println("Unrecognized type:" + type); //TODO
		}
	}
	
	/**
	 * toString() for logging and debugging
	 */
	public String toString()
	{
		return String.format("[num,timeSecs m1Small:%d,%d c1Medium:%d,%d m1Large"
				+ ":%d,%d m1XLarge:%d,%d c1XLarge:%d,%d disk:%d net:%d]",
				this.m1SmallNum, this.m1SmallTimeSecs, this.c1MediumNum,
				this.c1MediumTimeSecs, this.m1LargeNum, this.m1LargeTimeSecs,
				this.m1XLargeNum, this.m1XLargeTimeSecs, this.c1XLargeNum,
				this.c1XLargeTimeSecs, this.diskIoMegs, this.networkIoMegs);
	}
}
