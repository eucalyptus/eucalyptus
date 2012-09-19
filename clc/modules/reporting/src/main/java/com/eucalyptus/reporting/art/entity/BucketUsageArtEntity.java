package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class BucketUsageArtEntity
	implements ArtObject
{
	private long sizeGB;
	private long objectsNum = 0;
	private long gBSecs;
	private long numGetRequests;
	private long numPutRequests;
	
	public BucketUsageArtEntity()
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

	public long getObjectsNum()
	{
		return objectsNum;
	}

	public void setObjectsNum(long objectsNum)
	{
		this.objectsNum = objectsNum;
	}
	
	public void incrementObjectsNum()
	{
		this.objectsNum++;
	}

	public long getGBSecs()
	{
		return gBSecs;
	}

	public void setGBSecs(long gBSecs)
	{
		this.gBSecs = gBSecs;
	}

	public long getNumGetRequests()
	{
		return numGetRequests;
	}

	public void setNumGetRequests(long numGetRequests)
	{
		this.numGetRequests = numGetRequests;
	}

	public long getNumPutRequests()
	{
		return numPutRequests;
	}

	public void setNumPutRequests(long numPutRequests)
	{
		this.numPutRequests = numPutRequests;
	}
	
	
	
	
}
