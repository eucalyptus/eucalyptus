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

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_object_delete_events")
public class ReportingS3ObjectDeleteEvent
	extends AbstractPersistent
{
	@Column(name="s3_bucket_name", nullable=false)
	private String s3BucketName;
	@Column(name="s3_object_name", nullable=false)
	private String s3ObjectName;
	@Column(name="s3_object_size", nullable=false)
	private Long s3ObjectSize;
	@Column(name="s3_object_owner", nullable=false)
	private String s3ObjectOwnerId;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;

	protected ReportingS3ObjectDeleteEvent()
	{
		super();
		this.s3BucketName = null;
		this.s3ObjectName = null;
		this.s3ObjectSize = null;
		this.timestampMs = null;
		this.s3ObjectOwnerId = null;
	}	
	
	protected ReportingS3ObjectDeleteEvent(String s3BucketName, String s3ObjectName, Long s3ObjectSize, Long timestampMs, String s3ObjectOwnerId)
	{
		super();
		this.s3BucketName = s3BucketName;
		this.s3ObjectName = s3ObjectName;
		this.s3ObjectSize = s3ObjectSize;
		this.s3ObjectOwnerId = s3ObjectOwnerId;
		this.timestampMs = timestampMs;
	}

	public String getS3BucketName()
	{
		return s3BucketName;
	}
	
	public String getS3ObjectName()
	{
		return s3ObjectName;
	}
	
	public Long getS3ObjectSize()
	{
	    	return s3ObjectSize;
	}
	
	public String getS3ObjectOwnerId()
	{
	    	return s3ObjectOwnerId;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}

	@Override
	public String toString() {
	    return "ReportingS3ObjectDeleteEvent [s3BucketName=" + s3BucketName
		    + ", s3ObjectName=" + s3ObjectName + ", s3ObjectSize="
		    + s3ObjectSize + ", s3ObjectOwnerId=" + s3ObjectOwnerId
		    + ", timestampMs=" + timestampMs + "]";
	}

}
