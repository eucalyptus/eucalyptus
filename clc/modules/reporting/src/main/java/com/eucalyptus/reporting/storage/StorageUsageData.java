package com.eucalyptus.reporting.storage;

import javax.persistence.*;

@Embeddable
public class StorageUsageData
{
	@Column(name="volumes_num", nullable=false)
	protected Long volumesNum;
	@Column(name="volumes_gb", nullable=false)
	protected Long volumesGB;
	@Column(name="snapshot_num", nullable=false)
	protected Long snapshotsNum;
	@Column(name="snapshot_gb", nullable=false)
	protected Long snapshotsGB;
	@Column(name="objects_num", nullable=false)
	protected Long objectsNum;
	@Column(name="objects_gb", nullable=false)
	protected Long objectsGB;
	
	public StorageUsageData()
	{
		this.volumesNum   = new Long(0);
		this.volumesGB    = new Long(0);
		this.snapshotsNum = new Long(0);
		this.snapshotsGB  = new Long(0);
		this.objectsNum   = new Long(0);
		this.objectsGB    = new Long(0);
	}

	public StorageUsageData(Long volumesNum, Long volumesGB, Long snapshotsNum,
			Long snapshotsGB, Long objectsNum, Long objectsGB)
	{
		if (volumesNum == null || volumesGB == null || snapshotsNum == null
				|| snapshotsGB == null || objectsNum == null
				|| objectsGB == null)
		{
			throw new IllegalArgumentException("args can't be null");
		}
		this.volumesNum   = volumesNum;
		this.volumesGB    = volumesGB;
		this.snapshotsNum = snapshotsNum;
		this.snapshotsGB  = snapshotsGB;
		this.objectsNum   = objectsNum;
		this.objectsGB    = objectsGB;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}
	
	public Long getVolumesGB()
	{
		return volumesGB;
	}
	
	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}
	
	public Long getSnapshotsGB()
	{
		return snapshotsGB;
	}
	
	public Long getObjectsNum()
	{
		return objectsNum;
	}
	
	public Long getObjectsGB()
	{
		return objectsGB;
	}

	public void setVolumesNum(Long volumesNum)
	{
		if (volumesNum==null) throw new IllegalArgumentException("arg can't be null");
		this.volumesNum = volumesNum;
	}

	public void setVolumesGB(Long volumesGB)
	{
		if (volumesGB==null) throw new IllegalArgumentException("arg can't be null");
		this.volumesGB = volumesGB;
	}

	public void setSnapshotsNum(Long snapshotsNum)
	{
		if (snapshotsNum==null) throw new IllegalArgumentException("arg can't be null");
		this.snapshotsNum = snapshotsNum;
	}

	public void setSnapshotsGB(Long snapshotsGB)
	{
		if (snapshotsGB==null) throw new IllegalArgumentException("arg can't be null");
		this.snapshotsGB = snapshotsGB;
	}

	public void setObjectsNum(Long objectsNum)
	{
		if (objectsNum==null) throw new IllegalArgumentException("arg can't be null");
		this.objectsNum = objectsNum;
	}

	public void setObjectsGB(Long objectsGB)
	{
		if (objectsGB==null) throw new IllegalArgumentException("arg can't be null");
		this.objectsGB = objectsGB;
	}

	private static Long sumLongs(Long a, Long b)
	{
		return new Long(a.longValue() + b.longValue());
	}

	public StorageUsageData sumFrom(StorageUsageData other)
	{
		if (other == null) return null;
		return new StorageUsageData(
				sumLongs(this.volumesNum, other.volumesNum),
				sumLongs(this.volumesGB, other.volumesGB),
				sumLongs(this.snapshotsNum, other.snapshotsNum),
				sumLongs(this.snapshotsGB, other.snapshotsGB),
				sumLongs(this.objectsNum, other.objectsNum),
				sumLongs(this.objectsGB, other.objectsGB)
				);
	}

	public String toString()
	{
		return String.format("[vols:%d,volsGB:%d,snaps:%d,snapsGB:%d,objs:%d,"
				+ "objsGB:%d]", volumesNum, volumesGB, snapshotsNum,
				snapshotsGB, objectsNum, objectsGB);
	}


}
