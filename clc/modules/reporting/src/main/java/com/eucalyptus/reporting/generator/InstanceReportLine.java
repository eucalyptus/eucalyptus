package com.eucalyptus.reporting.generator;

import com.eucalyptus.reporting.instance.InstanceUsageSummary;

public class InstanceReportLine
{
	private final String label;
	private final String groupBy;
	private final InstanceUsageSummary summary;

	public InstanceReportLine(String label, String groupBy,
			InstanceUsageSummary summary)
	{
		this.label = label;
		this.groupBy = groupBy;
		this.summary = summary;
	}

	public String getLabel()
	{
		return label;
	}

	public String getGroupBy()
	{
		return groupBy;
	}

	public String getM1SmallNum()
	{
		return UnitUtil.getAmountString(summary.getM1SmallNum());
	}

	public String getM1SmallTimeSecs()
	{
		return UnitUtil.getTimeString(summary.getM1SmallTimeSecs());
	}

	public String getC1MediumNum()
	{
		return UnitUtil.getAmountString(summary.getC1MediumNum());
	}

	public String getC1MediumTimeSecs()
	{
		return UnitUtil.getTimeString(summary.getC1MediumTimeSecs());
	}

	public String getM1LargeNum()
	{
		return UnitUtil.getAmountString(summary.getM1LargeNum());
	}

	public String getM1LargeTimeSecs()
	{
		return UnitUtil.getTimeString(summary.getM1LargeTimeSecs());
	}

	public String getM1XLargeNum()
	{
		return UnitUtil.getAmountString(summary.getM1XLargeNum());
	}

	public String getM1XLargeTimeSecs()
	{
		return UnitUtil.getTimeString(summary.getM1XLargeTimeSecs());
	}

	public String getC1XLargeNum()
	{
		return UnitUtil.getAmountString(summary.getC1XLargeNum());
	}

	public String getC1XLargeTimeSecs()
	{
		return UnitUtil.getTimeString(summary.getC1XLargeTimeSecs());
	}

	public String getNetworkIoMegs()
	{
		return UnitUtil.getSizeString(summary.getNetworkIoMegs());
	}

	public String getDiskIoMegs()
	{
		return UnitUtil.getSizeString(summary.getDiskIoMegs());
	}
	
	
}
