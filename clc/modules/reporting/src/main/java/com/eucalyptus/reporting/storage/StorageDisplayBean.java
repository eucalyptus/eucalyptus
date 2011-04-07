package com.eucalyptus.reporting.storage;

import com.eucalyptus.reporting.units.*;

public class StorageDisplayBean
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final String label;
	private final String groupBy;
	private final StorageUsageSummary summary;
	private final Units units;

	StorageDisplayBean(String label, String groupBy,
			StorageUsageSummary summary, Units units)
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

}
