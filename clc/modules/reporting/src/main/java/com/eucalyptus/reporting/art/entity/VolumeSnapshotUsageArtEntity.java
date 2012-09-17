package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class VolumeSnapshotUsageArtEntity
	implements ArtObject
{
	private long sizeGB;
	private long gBSecs;
	private long snapshotNum;
	
	public VolumeSnapshotUsageArtEntity()
	{
	}
	
	public long getSizeGB()
	{
		return sizeGB;
	}

	public void setSizeGB(long sizeGB)
	{
		this.sizeGB = sizeGB;
	}

	public long getGBSecs()
	{
		return gBSecs;
	}

	public void setGBSecs(long gBSecs)
	{
		this.gBSecs = gBSecs;
	}

	public long getSnapshotNum()
	{
		return snapshotNum;
	}

	public void setSnapshotNum(long snapshotNum)
	{
		this.snapshotNum = snapshotNum;
	}

}
