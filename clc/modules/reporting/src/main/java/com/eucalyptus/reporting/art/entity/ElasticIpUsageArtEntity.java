package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

public class ElasticIpUsageArtEntity
	implements ArtObject
{
	private long durationMs;
	private long ipNum;
	
	public ElasticIpUsageArtEntity()
	{
		
	}

	public long getDurationMs()
	{
		return durationMs;
	}

	public void setDurationMs(long durationMs)
	{
		this.durationMs = durationMs;
	}

	public long getIpNum()
	{
		return ipNum;
	}

	public void setIpNum(long ipNum)
	{
		this.ipNum = ipNum;
	}

}
