package com.eucalyptus.reporting.storage;

public class StorageUsageSummary
{
	private Long volumesNum;
	private Long volumnsGbSecs;
	private Long snapshotsNum;
	private Long snapshotsGbSecs;
	private Long objectsNum;
	private Long objectsGbSecs;

	public StorageUsageSummary()
	{
		this.volumesNum      = new Long(0);
		this.volumnsGbSecs   = new Long(0);
		this.snapshotsNum    = new Long(0);
		this.snapshotsGbSecs = new Long(0);
		this.objectsNum      = new Long(0);
		this.objectsGbSecs   = new Long(0);
	}

	public StorageUsageSummary(Long volumesNum, Long volumnsGbSecs,
			Long snapshotsNum, Long snapshotsGbSecs, Long objectsNum,
			Long objectsGbSecs)
	{
		this.volumesNum      = volumesNum;
		this.volumnsGbSecs   = volumnsGbSecs;
		this.snapshotsNum    = snapshotsNum;
		this.snapshotsGbSecs = snapshotsGbSecs;
		this.objectsNum      = objectsNum;
		this.objectsGbSecs   = objectsGbSecs;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}

	public void setVolumesNum(Long volumesNum)
	{
		this.volumesNum = volumesNum;
	}

	public Long getVolumnsGbSecs()
	{
		return volumnsGbSecs;
	}

	public void setVolumnsGbSecs(Long volumnsGbSecs)
	{
		this.volumnsGbSecs = volumnsGbSecs;
	}

	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}

	public void setSnapshotsNum(Long snapshotsNum)
	{
		this.snapshotsNum = snapshotsNum;
	}

	public Long getSnapshotsGbSecs()
	{
		return snapshotsGbSecs;
	}

	public void setSnapshotsGbSecs(Long snapshotsGbSecs)
	{
		this.snapshotsGbSecs = snapshotsGbSecs;
	}

	public Long getObjectsNum()
	{
		return objectsNum;
	}

	public void setObjectsNum(Long objectsNum)
	{
		this.objectsNum = objectsNum;
	}

	public Long getObjectsGbSecs()
	{
		return objectsGbSecs;
	}

	public void setObjectsGbSecs(Long objectsGbSecs)
	{
		this.objectsGbSecs = objectsGbSecs;
	}
	
	void addUsage(StorageUsageData usageData, long durationMs)
	{
		
	}

}
