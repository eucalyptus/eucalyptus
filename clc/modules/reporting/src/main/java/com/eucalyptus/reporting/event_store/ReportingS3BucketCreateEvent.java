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
        @Column(name="bucket_size", nullable=false)
        protected Long bucketSize;
        
	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	protected ReportingS3BucketCreateEvent()
	{
		//NOTE: hibernate will overwrite these
		this.s3BucketName = null;
		this.userId = null;
		this.bucketSize = -1L;
		this.timestampMs = -1L;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	ReportingS3BucketCreateEvent(String s3BucketName, Long s3BucketSize, String userId,  Long timeInMs)
	{
		this.s3BucketName = s3BucketName;
		this.userId = userId;
		this.bucketSize = s3BucketSize;
		this.timestampMs = timeInMs;
	}


	public String getS3BucketName()
	{
		return this.s3BucketName;
	}

	public String getUserId()
	{
		return this.userId;
	}

	public long getBucketSize() 
	{
	    	return this.bucketSize;
	}

	public long getTimeInMs() 
	{
	    	return this.timestampMs;
	}

	@Override
	public String toString() {
	    return "ReportingS3BucketCreateEvent [s3BucketName=" + s3BucketName
		    + ", timestampMs=" + timestampMs + ", userId=" + userId
		    + ", bucketSize=" + bucketSize + "]";
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((bucketSize == null) ? 0 : bucketSize.hashCode());
	    result = prime * result
		    + ((s3BucketName == null) ? 0 : s3BucketName.hashCode());
	    result = prime * result
		    + ((timestampMs == null) ? 0 : timestampMs.hashCode());
	    result = prime * result
		    + ((userId == null) ? 0 : userId.hashCode());
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
	    ReportingS3BucketCreateEvent other = (ReportingS3BucketCreateEvent) obj;
	    if (bucketSize == null) {
		if (other.bucketSize != null)
		    return false;
	    } else if (!bucketSize.equals(other.bucketSize))
		return false;
	    if (s3BucketName == null) {
		if (other.s3BucketName != null)
		    return false;
	    } else if (!s3BucketName.equals(other.s3BucketName))
		return false;
	    if (timestampMs == null) {
		if (other.timestampMs != null)
		    return false;
	    } else if (!timestampMs.equals(other.timestampMs))
		return false;
	    if (userId == null) {
		if (other.userId != null)
		    return false;
	    } else if (!userId.equals(other.userId))
		return false;
	    return true;
	}
	
}
