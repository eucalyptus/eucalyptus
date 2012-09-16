package com.eucalyptus.reporting.art.entity;

import java.util.*;

import com.eucalyptus.reporting.art.ArtObject;

public class ElasticIpArtEntity
	implements ArtObject
{
	final ElasticIpUsageArtEntity usage;
	final Map<String, ElasticIpUsageArtEntity> instanceAttachments;
	
	public ElasticIpArtEntity()
	{
		this.usage = new ElasticIpUsageArtEntity();
		this.instanceAttachments = new HashMap<String, ElasticIpUsageArtEntity>();
	}
	
	public ElasticIpUsageArtEntity getUsage()
	{
		return this.usage;
	}
	
	/**
	 * instanceId -> usage 
	 */
	public Map<String, ElasticIpUsageArtEntity> getInstanceAttachments()
	{
		return this.instanceAttachments;
	}
	
}
