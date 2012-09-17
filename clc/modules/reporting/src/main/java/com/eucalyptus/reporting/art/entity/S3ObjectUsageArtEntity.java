package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class S3ObjectUsageArtEntity
	implements ArtObject
{
	private long objectsNum;
	private long sizeGB;
	private long gBsecs;
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
	
	public long getGBsecs()
	{
		return gBsecs;
	}

	public void setGBsecs(long gBsecs)
	{
		this.gBsecs = gBsecs;
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

	public long getObjectsNum()
	{
		return objectsNum;
	}

	public void setObjectsNum(long objectsNum)
	{
		this.objectsNum = objectsNum;
	}

	
	
}
