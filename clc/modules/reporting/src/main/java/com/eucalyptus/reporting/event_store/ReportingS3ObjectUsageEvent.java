package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_object_usage_events")
public class ReportingS3ObjectUsageEvent
	extends AbstractPersistent 
{
	@Column(name="bucket_name", nullable=false)
	protected String bucketName;
	@Column(name="object_name", nullable=false)
	protected String objectName;	
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="cumulative_read_megs", nullable=true)
	protected Long cumulativeReadMegs;
	@Column(name="cumulative_written_megs", nullable=true)
	protected Long cumulativeWrittenMegs;
	@Column(name="cumulative_get_requests", nullable=true)
	protected Long cumulativeGetRequests;
	@Column(name="cumulative_put_requsts", nullable=true)
	protected Long cumulativePutRequests;


	protected ReportingS3ObjectUsageEvent()
	{
		//hibernate will override these thru reflection despite finality
		this.timestampMs = null;
		this.cumulativeReadMegs = null;
		this.cumulativeWrittenMegs = null;
		this.cumulativeGetRequests = null;
		this.cumulativePutRequests = null;
	}

	ReportingS3ObjectUsageEvent(String bucketName, String objectName, Long timestampMs,
			Long cumulativeNetIoMegs, Long cumulateiveDiskIoMegs, Long cumulativeGetRequests,
			Long cumulativePutRequests)
	{
		if (timestampMs == null)
			throw new IllegalArgumentException("timestampMs can't be null");
		if (bucketName == null)
			throw new IllegalArgumentException("bucketName can't be null");
		if (objectName == null)
			throw new IllegalArgumentException("objectName can't be null");
		this.timestampMs = timestampMs;
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.cumulativeReadMegs = cumulativeNetIoMegs;
		this.cumulativeWrittenMegs = cumulateiveDiskIoMegs;
		this.cumulativeGetRequests = cumulativeGetRequests;
		this.cumulativePutRequests = cumulativePutRequests;
	}

	public Long getTimestampMs()
	{
		return timestampMs;
	}
	
	public String getBucketName()
	{
		return bucketName;
	}

	public String getObjectName()
	{
		return objectName;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeReadMegs()
	{
		return cumulativeReadMegs;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeWrittenMegs()
	{
		return cumulativeWrittenMegs;
	}

	public Long getCumulativeGetRequests()
	{
		return cumulativeGetRequests;
	}
	
	public Long getCumulativePutRequests()
	{
		return cumulativePutRequests;
	}
	
	@Override
	public String toString()
	{
		return "[timestamp: " + this.timestampMs + " cumulReadMegs:" + this.cumulativeReadMegs
		+ " cumulWrittenMegs:" + this.cumulativeWrittenMegs + " cumulGetRequests:"
		+ this.cumulativeGetRequests + " cumulPutRequests:" + this.cumulativePutRequests + "]";
	}
	

  /**
   * NOTE:IMPORTANT: this method has default visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   * 
   * TODO: remove this if possible.
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getEntityId( ) {
    return this.getId( );
  }

}
