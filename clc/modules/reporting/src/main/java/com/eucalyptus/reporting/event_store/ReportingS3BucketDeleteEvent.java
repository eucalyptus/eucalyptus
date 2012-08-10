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

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_bucket_delete_events")
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
