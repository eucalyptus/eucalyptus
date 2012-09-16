package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class VolumeSnapshotUsageArtEntity
	implements ArtObject
{
	private long sizeGB;
	private long durationMs;
	
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

	public long getDurationMs()
	{
		return durationMs;
	}

	public void setDurationMs(long durationMs)
	{
		this.durationMs = durationMs;
	}


}
