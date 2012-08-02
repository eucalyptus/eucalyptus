/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

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
	

}
