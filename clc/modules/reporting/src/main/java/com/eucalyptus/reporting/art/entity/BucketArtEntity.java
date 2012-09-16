package com.eucalyptus.reporting.art.entity;

import java.util.*;

import com.eucalyptus.reporting.art.ArtObject;

public class BucketArtEntity
	implements ArtObject
{
	private final Map<String, S3ObjectUsageArtEntity> objects;
	private final S3ObjectUsageArtEntity totalUsage;
	
	public BucketArtEntity()
	{
		this.objects = new HashMap<String, S3ObjectUsageArtEntity>();
		this.totalUsage = new S3ObjectUsageArtEntity();
	}

	public Map<String, S3ObjectUsageArtEntity> getObjects()
	{
		return objects;
	}
	
	public S3ObjectUsageArtEntity getTotalUsage()
	{
		return totalUsage;
	}
	
}
