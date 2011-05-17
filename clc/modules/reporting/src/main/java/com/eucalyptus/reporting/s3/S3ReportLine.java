package com.eucalyptus.reporting.s3;

import com.eucalyptus.reporting.units.*;

public class S3ReportLine
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final String label;
	private final String groupBy;
	private final S3UsageSummary summary;
	private final Units units;

	S3ReportLine(String label, String groupBy,
			S3UsageSummary summary, Units units)
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

	public Long getBucketsNumMax()
	{
		return summary.getBucketsNumMax();
	}

	public Long getObjectsSizeMax()
	{
		return UnitUtil.convertSize(summary.getObjectsMegsMax(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getObjectsSizeTime()
	{
		return UnitUtil.convertSizeTime(summary.getObjectsMegsSecs(),
				INTERNAL_UNITS.getSizeTimeSizeUnit(), units.getSizeTimeSizeUnit(),
				INTERNAL_UNITS.getSizeTimeTimeUnit(), units.getSizeTimeTimeUnit());
	}
	
	public Units getUnits()
	{
		return units;
	}
	
	void addUsage(S3UsageSummary summary)
	{
		this.summary.addUsage(summary);
	}

}
