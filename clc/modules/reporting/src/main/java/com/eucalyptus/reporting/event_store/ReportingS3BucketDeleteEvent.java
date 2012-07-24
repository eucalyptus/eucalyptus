package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_bucket_create_events")
public class ReportingS3BucketDeleteEvent
{
	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;

	protected ReportingS3BucketDeleteEvent()
	{
		super();
		this.s3BucketName = null;
		this.timestampMs = null;
	}

	protected ReportingS3BucketDeleteEvent(String s3BucketName, Long timestampMs)
	{
		super();
		this.s3BucketName = s3BucketName;
		this.timestampMs = timestampMs;
	}

	public String getS3BucketName()
	{
		return s3BucketName;
	}

	public Long getTimestampMs()
	{
		return timestampMs;
	}

	
}
