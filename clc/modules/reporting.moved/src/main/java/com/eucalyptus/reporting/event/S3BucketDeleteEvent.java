package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="s3_bucket_delete")
public class S3BucketDeleteEvent
	implements S3Event
{
	@Id
	@Column(name="bucket_id")
	private Long bucketId;
	@Column(name="deleted_time_ms")
	private Long deletedTimeMs;

	public S3BucketDeleteEvent()
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

	public Long getDeletedTimeMs()
	{
		return this.deletedTimeMs;
	}

	public void setDeletedTimeMs(Long deletedTimeMs)
	{
		this.deletedTimeMs = deletedTimeMs;
	}

	public boolean requiresReliableTransmission()
	{
		return true;
	}
}
