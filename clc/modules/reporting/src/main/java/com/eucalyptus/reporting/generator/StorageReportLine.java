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

	/**
	 * @return A string with appropriate size and units; e.g. "8 GB"
	 */
	public String getVolumesSizeMax()
	{
		return UnitUtil.getSizeString(summary.getVolumesMegsMax());
	}

	/**
	 * @return A string with appropriate size and units; e.g. "4k"
	 */
	public String getVolumesMegsHrs()
	{
		return UnitUtil.getAmountString(summary.getVolumesMegsHrs());
	}

	/**
	 * @return A string with appropriate size and units; e.g. "8 GB"
	 */
	public String getSnapshotsSizeMax()
	{
		return UnitUtil.getSizeString(summary.getSnapshotsMegsMax());
	}

	/**
	 * @return A string with appropriate size and units; e.g. "4k"
	 */
	public String getSnapshotsMegsHrs()
	{
		return UnitUtil.getSizeString(summary.getSnapshotsMegsHrs());
	}

	/**
	 * @return A string with appropriate size and units; e.g. "8 GB"
	 */
	public String getObjectsSizeMax()
	{
		return UnitUtil.getSizeString(summary.getObjectsMegsMax());
	}

	/**
	 * @return A string with appropriate size and units; e.g. "4k"
	 */
	public String getObjectsMegsHrs()
	{
		return UnitUtil.getSizeString(summary.getObjectsMegsHrs());
	}

}
