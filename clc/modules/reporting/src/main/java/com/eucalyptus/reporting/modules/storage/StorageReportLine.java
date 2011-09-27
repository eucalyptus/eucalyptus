package com.eucalyptus.reporting.modules.storage;

import com.eucalyptus.reporting.ReportLine;
import com.eucalyptus.reporting.units.*;

public class StorageReportLine
	implements Comparable<StorageReportLine>, ReportLine
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final StorageReportLineKey key;
	private final StorageUsageSummary summary;
	private final Units units;

	StorageReportLine(StorageReportLineKey key,
			StorageUsageSummary summary, Units units)
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

	public Long getVolumesSizeMax()
	{
		return UnitUtil.convertSize(summary.getVolumesMegsMax(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getVolumesSizeTime()
	{
		return UnitUtil.convertSizeTime(summary.getVolumesMegsSecs(),
				INTERNAL_UNITS.getSizeTimeSizeUnit(), units.getSizeTimeSizeUnit(),
				INTERNAL_UNITS.getSizeTimeTimeUnit(), units.getSizeTimeTimeUnit());
	}

	public Long getSnapshotsSizeMax()
	{
		return UnitUtil.convertSize(summary.getSnapshotsMegsMax(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getSnapshotsSizeTime()
	{
		return UnitUtil.convertSizeTime(summary.getSnapshotsMegsSecs(),
				INTERNAL_UNITS.getSizeTimeSizeUnit(), units.getSizeTimeSizeUnit(),
				INTERNAL_UNITS.getSizeTimeTimeUnit(), units.getSizeTimeTimeUnit());
	}

	public Units getUnits()
	{
		return units;
	}
	
	void addUsage(StorageUsageSummary summary)
	{
		this.summary.addUsage(summary);
	}

	public String toString()
	{
		return String.format("[label:%s,groupBy:%s,volSizeMax:%d,volSizeTime:%d,snapsSizeMax:%d,snapsSizeTime:%d]",
						getLabel(), getGroupBy(), getVolumesSizeMax(),
						getVolumesSizeTime(), getSnapshotsSizeMax(),
						getSnapshotsSizeTime());
	}

	@Override
	public int compareTo(StorageReportLine other)
	{
		return key.compareTo(other.key);
	}


}
