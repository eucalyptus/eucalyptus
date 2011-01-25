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

	public Long getM1SmallNum()
	{
		return summary.getM1SmallNum();
	}

	public Long getM1SmallTimeMs()
	{
		return summary.getM1SmallTimeMs();
	}

	public Long getC1MediumNum()
	{
		return summary.getC1MediumNum();
	}

	public Long getC1MediumTimeMs()
	{
		return summary.getC1MediumTimeMs();
	}

	public Long getM1LargeNum()
	{
		return summary.getM1LargeNum();
	}

	public Long getM1LargeTimeMs()
	{
		return summary.getM1LargeTimeMs();
	}

	public Long getM1XLargeNum()
	{
		return summary.getM1XLargeNum();
	}

	public Long getM1XLargeTimeMs()
	{
		return summary.getM1XLargeTimeMs();
	}

	public Long getC1XLargeNum()
	{
		return summary.getC1XLargeNum();
	}

	public Long getC1XLargeTimeMs()
	{
		return summary.getC1XLargeTimeMs();
	}

	public Long getNetworkIoMegs()
	{
		return summary.getNetworkIoMegs();
	}

	public Long getDiskIoMegs()
	{
		return summary.getDiskIoMegs();
	}
	
	
}
