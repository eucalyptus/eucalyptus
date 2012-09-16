package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class S3ObjectUsageArtEntity
	implements ArtObject
{
	private long sizeGB;
	private long durationMs;
	private long getRequestsNum;
	private long putRequestsNum;
	
	public S3ObjectUsageArtEntity()
	{
	}
	
	public S3ObjectUsageArtEntity(long sizeGB)
	{
		this.sizeGB = sizeGB;		
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

	public long getGetRequestsNum()
	{
		return getRequestsNum;
	}

	public void setGetRequestsNum(long getRequestsNum)
	{
		this.getRequestsNum = getRequestsNum;
	}

	public long getPutRequestsNum()
	{
		return putRequestsNum;
	}

	public void setPutRequestsNum(long putRequestsNum)
	{
		this.putRequestsNum = putRequestsNum;
	}

}
