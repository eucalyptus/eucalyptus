package com.eucalyptus.reporting.storage;

import javax.persistence.*;

@Embeddable
public class StorageUsageData
{
	@Column(name="volumes_num", nullable=true)
	protected Long volumesNum;
	@Column(name="volumes_gb", nullable=true)
	protected Long volumesGb; //GigaBYTES, not gigabits; following java caps
	@Column(name="snapshot_num", nullable=true)
	protected Long snapshotsNum;
	@Column(name="snapshot_gb", nullable=true)
	protected Long snapshotsGb;
	@Column(name="objects_num", nullable=true)
	protected Long objectsNum;
	@Column(name="objects_gb", nullable=true)
	protected Long objectsGb;
	
	public StorageUsageData()
	{
		this.volumesNum   =  null;
		this.volumesGb    =  null;
		this.snapshotsNum =  null;
		this.snapshotsGb  =  null;
		this.objectsNum   =  null;
		this.objectsGb    =  null;
	}

	public StorageUsageData(Long volumesNum, Long volumesGb, Long snapshotsNum,
			Long snapshotsGb, Long objectsNum, Long objectsGb)
	{
		this.volumesNum = volumesNum;
		this.volumesGb = volumesGb;
		this.snapshotsNum = snapshotsNum;
		this.snapshotsGb = snapshotsGb;
		this.objectsNum = objectsNum;
		this.objectsGb = objectsGb;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}
	
	public Long getVolumesGb()
	{
		return volumesGb;
	}
	
	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}
	
	public Long getSnapshotsGb()
	{
		return snapshotsGb;
	}
	
	public Long getObjectsNum()
	{
		return objectsNum;
	}
	
	public Long getObjectsGb()
	{
		return objectsGb;
	}

	public void setVolumesNum(Long volumesNum)
	{
		this.volumesNum = volumesNum;
	}

	public void setVolumesGb(Long volumesGb)
	{
		this.volumesGb = volumesGb;
	}

	public void setSnapshotsNum(Long snapshotsNum)
	{
		this.snapshotsNum = snapshotsNum;
	}

	public void setSnapshotsGb(Long snapshotsGb)
	{
		this.snapshotsGb = snapshotsGb;
	}

	public void setObjectsNum(Long objectsNum)
	{
		this.objectsNum = objectsNum;
	}

	public void setObjectsGb(Long objectsGb)
	{
		this.objectsGb = objectsGb;
	}

	private static Long sumWithNull(Long a, Long b)
	{
		return (a != null && b != null)
		  ? new Long(a.longValue() + b.longValue()) : null;
	}

	public StorageUsageData sumFrom(StorageUsageData other)
	{
		if (other == null) return null;
		return new StorageUsageData(
				sumWithNull(this.volumesNum, other.volumesNum),
				sumWithNull(this.volumesGb, other.volumesGb),
				sumWithNull(this.snapshotsNum, other.snapshotsNum),
				sumWithNull(this.snapshotsGb, other.snapshotsGb),
				sumWithNull(this.objectsNum, other.objectsNum),
				sumWithNull(this.objectsGb, other.objectsGb)
				);
	}

	public String toString()
	{
		return String.format("[vols:%d,volsGb:%d,snaps:%d,snapsGb:%d,objs:%d,"
				+ "objsGb:%d]", volumesNum, volumesGb, snapshotsNum,
				snapshotsGb, objectsNum, objectsGb);
	}


}
