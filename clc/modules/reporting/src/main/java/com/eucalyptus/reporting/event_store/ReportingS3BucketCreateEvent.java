package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_bucket_create_events")
public class ReportingS3BucketCreateEvent
	extends AbstractPersistent
{
	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="user_id", nullable=false)
	protected String userId;
	@Column(name="availability_zone")
	protected String availabilityZone;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	protected ReportingS3BucketCreateEvent()
	{
		//NOTE: hibernate will overwrite these
		this.s3BucketName = null;
		this.userId = null;
		this.availabilityZone = null;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	ReportingS3BucketCreateEvent(String s3BucketName, String userId, String availabilityZone)
	{
		this.s3BucketName = s3BucketName;
		this.userId = userId;
		this.availabilityZone = availabilityZone;
	}


	public String getS3BucketName()
	{
		return this.s3BucketName;
	}

	public String getUserId()
	{
		return this.userId;
	}

	public String getAvailabilityZone()
	{
		return this.availabilityZone;
	}

	@Override
	public int hashCode()
	{
		return (s3BucketName == null) ? 0 : s3BucketName.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		ReportingS3BucketCreateEvent other = (ReportingS3BucketCreateEvent) obj;
		if (s3BucketName == null) {
			if (other.s3BucketName != null)
				return false;
		} else if (!s3BucketName.equals(other.s3BucketName))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "[s3BucketName:" + this.s3BucketName + " userId:" + this.userId + "]";
	}


}
