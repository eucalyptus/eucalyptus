package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="s3_object_create")
public class S3ObjectCreateEvent
	implements S3Event
{
	@Id
	@Column(name="object_id")
	Long objectId;
	@Column(name="bucket_name")
	private String bucketName;
	@Column(name="name")
	private String name;
	@Column(name="size")
	private Long size;
	@Column(name="created_time_ms")
	private Long createdTimeMs;

	public S3ObjectCreateEvent()
	{
	}

	public Long getObjectId()
	{
		return objectId;
	}

	public void setObjectId(Long objectId)
	{
		this.objectId = objectId;
	}

	public String getBucketName()
	{
		return this.bucketName;
	}

	public void setBucketName(String bucketName)
	{
		this.bucketName = bucketName;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Long getSize()
	{
		return this.size;
	}

	public void setSize(Long size)
	{
		this.size = size;
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

