package com.eucalyptus.reporting.instance;

import com.eucalyptus.reporting.units.*;

public class InstanceDisplayBean
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final String label;
	private final String groupBy;
	private final InstanceUsageSummary summary;
	private final Units units;

	InstanceDisplayBean(String label, String groupBy,
			InstanceUsageSummary summary, Units units)
	{
		this.label = label;
		this.groupBy = groupBy;
		this.summary = summary;
		this.units = units;
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

	public Long getM1SmallTime()
	{
		return UnitUtil.convertTime(summary.getM1SmallTimeSecs(),
				INTERNAL_UNITS.getTimeUnit(), units.getTimeUnit());
	}

	public Long getC1MediumNum()
	{
		return summary.getC1MediumNum();
	}

	public Long getC1MediumTime()
	{
		return UnitUtil.convertTime(summary.getC1MediumTimeSecs(),
				INTERNAL_UNITS.getTimeUnit(), units.getTimeUnit());
	}

	public Long getM1LargeNum()
	{
		return summary.getM1LargeNum();
	}

	public Long getM1LargeTime()
	{
		return UnitUtil.convertTime(summary.getM1LargeTimeSecs(),
				INTERNAL_UNITS.getTimeUnit(), units.getTimeUnit());
	}

	public Long getM1XLargeNum()
	{
		return summary.getM1XLargeNum();
	}

	public Long getM1XLargeTime()
	{
		return UnitUtil.convertTime(summary.getM1XLargeTimeSecs(),
				INTERNAL_UNITS.getTimeUnit(), units.getTimeUnit());
	}

	public Long getC1XLargeNum()
	{
		return summary.getC1XLargeNum();
	}

	public Long getC1XLargeTime()
	{
		return UnitUtil.convertTime(summary.getC1XLargeTimeSecs(),
				INTERNAL_UNITS.getTimeUnit(), units.getTimeUnit());
	}

	public Long getNetworkIoSize()
	{
		return UnitUtil.convertSize(summary.getNetworkIoMegs(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getDiskIoSize()
	{
		return UnitUtil.convertSize(summary.getDiskIoMegs(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

}
