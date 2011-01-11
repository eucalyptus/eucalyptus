package com.eucalyptus.reporting.storage;

public class StorageUsageSummary
{
	private Long volumesNum;
	private Long snapshotsNum;
	private Long volumesGbHours;
	private Long snapshotsGbHours;
	private Long objectsNum;
	private Long objectsGbHours;

	public StorageUsageSummary()
	{
		this.volumesNum       = new Long(0);
		this.snapshotsNum     = new Long(0);
		this.volumesGbHours   = new Long(0);
		this.snapshotsGbHours = new Long(0);
		this.objectsNum       = new Long(0);
		this.objectsGbHours   = new Long(0);
	}

	public StorageUsageSummary(Long volumesNum, Long snapshotsNum,
			Long volumesGbHours, Long snapshotsGbHours, Long objectsNum,
			Long objectsGbHours)
	{
		this.volumesNum       = volumesNum;
		this.snapshotsNum     = snapshotsNum;
		this.volumesGbHours   = volumesGbHours;
		this.snapshotsGbHours = snapshotsGbHours;
		this.objectsNum       = objectsNum;
		this.objectsGbHours   = objectsGbHours;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}

	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}

	public Long getVolumesGbHours()
	{
		return volumesGbHours;
	}

	public Long getSnapshotsGbHours()
	{
		return snapshotsGbHours;
	}

	public Long getObjectsNum()
	{
		return objectsNum;
	}

	public Long getObjectsGbHours()
	{
		return objectsGbHours;
	}

}
