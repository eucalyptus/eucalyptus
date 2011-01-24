package com.eucalyptus.reporting.generator;

import com.eucalyptus.reporting.storage.StorageUsageSummary;

public class StorageReportLine
{
	private final String label;
	private final String groupBy;
	private final StorageUsageSummary summary;

	public StorageReportLine(String label, String groupBy,
			StorageUsageSummary summary)
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

	public long getVolumesGbMax()
	{
		return summary.getVolumesGbMax();
	}

	public long getVolumesGbSecs()
	{
		return summary.getVolumesGbSecs();
	}

	public long getSnapshotsGbMax()
	{
		return summary.getSnapshotsGbMax();
	}

	public long getSnapshotsGbSecs()
	{
		return summary.getSnapshotsGbSecs();
	}

	public long getObjectsGbMax()
	{
		return summary.getObjectsGbMax();
	}

	public long getObjectsGbSecs()
	{
		return summary.getObjectsGbSecs();
	}
	
}
