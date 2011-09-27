package com.eucalyptus.reporting.modules.s3;

import com.eucalyptus.reporting.ReportLine;
import com.eucalyptus.reporting.units.*;

public class S3ReportLine
	implements Comparable<S3ReportLine>, ReportLine
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final S3ReportLineKey key;
	private final S3UsageSummary summary;
	private final Units units;

	S3ReportLine(S3ReportLineKey key,
			S3UsageSummary summary, Units units)
	{
		this.key = key;
		this.summary = summary;
		this.units = units;
	}

	public String getLabel()
	{
		return key.getLabel();
	}

	public String getGroupBy()
	{
		return key.getGroupByLabel();
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

	@Override
	public int compareTo(S3ReportLine other)
	{
		return key.compareTo(other.key);
	}

}
