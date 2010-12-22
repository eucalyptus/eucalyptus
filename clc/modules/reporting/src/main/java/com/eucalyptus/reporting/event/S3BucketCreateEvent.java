package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="s3_bucket_create")
public class S3BucketCreateEvent
	implements S3Event
{
	@Id
	@Column(name="bucket_id")
	private Long bucketId;
	@Column(name="name")
	private String name;
	@Column(name="owner")
	private String owner;
	@Column(name="created_time_ms")
	private Long createdTimeMs;

	public S3BucketCreateEvent()
	{
	}

	public Long getBucketId()
	{
		return bucketId;
	}


	public void setBucketId(Long bucketId)
	{
		this.bucketId = bucketId;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOwner()
	{
		return this.owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public Long getCreatedTimeMs()
	{
		return this.createdTimeMs;
	}

	public void setCreatedTimeMs(Long createdTimeMs)
	{
		this.createdTimeMs = createdTimeMs;
	}

	public boolean requiresReliableTransmission()
	{
		return true;
	}
}

