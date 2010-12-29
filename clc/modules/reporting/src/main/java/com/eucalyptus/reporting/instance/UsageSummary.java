package com.eucalyptus.reporting.instance;

public class UsageSummary
{
	private Long m1SmallNum;
	private Long m1SmallTimeMs;
	private Long c1MediumNum;
	private Long c1MediumTimeMs;
	private Long m1LargeNum;
	private Long m1LargeTimeMs;
	private Long m1XLargeNum;
	private Long m1XLargeTimeMs;
	private Long c1XLargeNum;
	private Long c1XLargeTimeMs;
	private Long networkIoMegs;
	private Long diskIoMegs;
	
	public UsageSummary()
	{
		this.m1SmallNum = new Long(0);
		this.m1SmallTimeMs = new Long(0);
		this.c1MediumNum = new Long(0);
		this.c1MediumTimeMs = new Long(0);
		this.m1LargeNum = new Long(0);
		this.m1LargeTimeMs = new Long(0);
		this.m1XLargeNum = new Long(0);
		this.m1XLargeTimeMs = new Long(0);
		this.c1XLargeNum = new Long(0);
		this.c1XLargeTimeMs = new Long(0);
		this.networkIoMegs = new Long(0);
		this.diskIoMegs = new Long(0);
	}

	public UsageSummary(Long m1SmallNum, Long m1SmallTimeMs,
			Long c1MediumNum, Long c1MediumTimeMs, Long m1LargeNum,
			Long m1LargeTimeMs, Long m1XLargeNum, Long m1XLargeTimeMs,
			Long c1XLargeNum, Long c1XLargeTimeMs, Long networkIoMegs,
			Long diskIoMegs)
	{
		//Check that no args are null
		if (m1SmallNum == null || m1SmallTimeMs == null || c1MediumNum == null
			|| c1MediumTimeMs == null || m1LargeNum == null || m1LargeTimeMs == null
			|| m1XLargeNum == null || m1XLargeTimeMs == null || c1XLargeNum == null
			|| c1XLargeTimeMs == null || networkIoMegs == null || diskIoMegs == null)
		{
			throw new IllegalArgumentException("ctor args cannot be null");
		}
		
		this.m1SmallNum = m1SmallNum;
		this.m1SmallTimeMs = m1SmallTimeMs;
		this.c1MediumNum = c1MediumNum;
		this.c1MediumTimeMs = c1MediumTimeMs;
		this.m1LargeNum = m1LargeNum;
		this.m1LargeTimeMs = m1LargeTimeMs;
		this.m1XLargeNum = m1XLargeNum;
		this.m1XLargeTimeMs = m1XLargeTimeMs;
		this.c1XLargeNum = c1XLargeNum;
		this.c1XLargeTimeMs = c1XLargeTimeMs;
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

	public Long getM1SmallTimeMs()
	{
		return m1SmallTimeMs;
	}

	public void setM1SmallTimeMs(Long m1SmallTimeMs)
	{
		if (m1SmallTimeMs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1SmallTimeMs = m1SmallTimeMs;
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

	public Long getC1MediumTimeMs()
	{
		return c1MediumTimeMs;
	}

	public void setC1MediumTimeMs(Long c1MediumTimeMs)
	{
		if (c1MediumTimeMs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1MediumTimeMs = c1MediumTimeMs;
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

	public Long getM1LargeTimeMs()
	{
		return m1LargeTimeMs;
	}

	public void setM1LargeTimeMs(Long m1LargeTimeMs)
	{
		if (m1LargeTimeMs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1LargeTimeMs = m1LargeTimeMs;
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

	public Long getM1XLargeTimeMs()
	{
		return m1XLargeTimeMs;
	}

	public void setM1XLargeTimeMs(Long m1XLargeTimeMs)
	{
		if (m1XLargeTimeMs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1XLargeTimeMs = m1XLargeTimeMs;
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

	public Long getC1XLargeTimeMs()
	{
		return c1XLargeTimeMs;
	}

	public void setC1XLargeTimeMs(Long c1XLargeTimeMs)
	{
		if (c1XLargeTimeMs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1XLargeTimeMs = c1XLargeTimeMs;
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
		long timeMs = period.getEndingMs() - period.getBeginningMs();

		//Autoboxing should work because we prevented nulls everywhere
		if (type.equalsIgnoreCase("m1small")) {
			this.m1SmallNum = this.m1SmallNum + 1;
			this.m1SmallTimeMs = this.m1SmallTimeMs + timeMs;
		} else if (type.equalsIgnoreCase("c1medium")) {
			this.c1MediumNum = this.c1MediumNum + 1;
			this.c1MediumTimeMs = this.c1MediumTimeMs + timeMs;
		} else if (type.equalsIgnoreCase("m1large")) {
			this.m1LargeNum = this.m1LargeNum + 1;
			this.m1LargeTimeMs = this.m1LargeTimeMs + timeMs;
		} else if (type.equalsIgnoreCase("m1xlarge")) {
			this.m1XLargeNum = this.m1XLargeNum + 1;
			this.m1XLargeTimeMs = this.m1XLargeTimeMs + timeMs;
		} else if (type.equalsIgnoreCase("c1xlarge")) {
			this.c1XLargeNum = this.c1XLargeNum + 1;
			this.c1XLargeTimeMs = this.c1XLargeTimeMs + timeMs;
		} else {
			System.err.println("Unrecognized type:" + type);
		}
	}
	
	/**
	 * toString() for logging and debugging
	 */
	public String toString()
	{
		return String.format("[num,timeMs m1Small:%d,%d c1Medium:%d,%d m1Large"
				+ ":%d,%d m1XLarge:%d,%d c1XLarge:%d,%d disk:%d net:%d]",
				this.m1SmallNum, this.m1SmallTimeMs, this.c1MediumNum,
				this.c1MediumTimeMs, this.m1LargeNum, this.m1LargeTimeMs,
				this.m1XLargeNum, this.m1XLargeTimeMs, this.c1XLargeNum,
				this.c1XLargeTimeMs, this.diskIoMegs, this.networkIoMegs);
	}
}
