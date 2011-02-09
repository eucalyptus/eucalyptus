package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="s3_object_delete")
public class S3ObjectDeleteEvent
	implements S3Event
{
	@Id
	@Column(name="object_id")
	Long objectId;
	@Column(name="deleted_time_ms")
	private Long deletedTimeMs;

	public S3ObjectDeleteEvent()
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
