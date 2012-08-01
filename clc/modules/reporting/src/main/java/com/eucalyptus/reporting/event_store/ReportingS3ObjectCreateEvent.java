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

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_object_create_events")
public class ReportingS3ObjectCreateEvent
	extends AbstractPersistent
{
	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="s3_object_name", nullable=false)
	protected String s3ObjectName;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="user_id", nullable=false)
	protected String userId;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	protected ReportingS3ObjectCreateEvent()
	{
		//NOTE: hibernate will overwrite these
		this.s3ObjectName = null;
		this.s3BucketName = null;
		this.timestampMs = null;
		this.userId = null;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	ReportingS3ObjectCreateEvent(String s3BucketName, String s3ObjectName, Long timestampMs, String userId)
	{
		this.s3BucketName = s3BucketName;
		this.s3ObjectName = s3ObjectName;
		this.timestampMs = timestampMs;
		this.userId = userId;
	}

	public String getS3BucketName()
	{
		return this.s3BucketName;
	}

	public String getS3ObjectName()
	{
		return this.s3ObjectName;
	}

	public String getUserId()
	{
		return this.userId;
	}
	
	public Long getTimestampMs()
	{
		return this.timestampMs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((s3BucketName == null) ? 0 : s3BucketName.hashCode());
		result = prime * result
				+ ((s3ObjectName == null) ? 0 : s3ObjectName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportingS3ObjectCreateEvent other = (ReportingS3ObjectCreateEvent) obj;
		if (s3BucketName == null) {
			if (other.s3BucketName != null)
				return false;
		} else if (!s3BucketName.equals(other.s3BucketName))
			return false;
		if (s3ObjectName == null) {
			if (other.s3ObjectName != null)
				return false;
		} else if (!s3ObjectName.equals(other.s3ObjectName))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "[s3BucketName:" + s3BucketName + " s3ObjectName:" + this.s3ObjectName + " userId:" + this.userId + "]";
	}


}
