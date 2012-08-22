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
@Table(name="reporting_s3_bucket_delete_events")
public class ReportingS3BucketDeleteEvent
	extends AbstractPersistent
{
	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="s3_bucket_size", nullable=false)
	protected Long s3BucketSize;
	@Column(name="s3_bucket_userId", nullable=false)
	protected String s3userId;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;

	protected ReportingS3BucketDeleteEvent()
	{
		super();
		this.timestampMs = null;
		this.s3BucketName = null;
		this.s3BucketSize = null;
		this.s3userId = null;
	}

	protected ReportingS3BucketDeleteEvent(String s3BucketName, Long s3BucketSize, String userId, Long timeInMs)
	{
		super();
		this.s3BucketName = s3BucketName;
    this.s3BucketSize = s3BucketSize;
    this.s3userId = userId;
    this.timestampMs = timeInMs;
	}

	public String getS3BucketName()
	{
		return s3BucketName;
	}

	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public Long getS3BucketSize() {
	    return s3BucketSize;
	}


	public String getS3userId() {
	    return s3userId;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((s3BucketName == null) ? 0 : s3BucketName.hashCode());
	    result = prime * result
		    + ((s3BucketSize == null) ? 0 : s3BucketSize.hashCode());
	    result = prime * result
		    + ((s3userId == null) ? 0 : s3userId.hashCode());
	    result = prime * result
		    + ((timestampMs == null) ? 0 : timestampMs.hashCode());
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
	    ReportingS3BucketDeleteEvent other = (ReportingS3BucketDeleteEvent) obj;
	    if (s3BucketName == null) {
		if (other.s3BucketName != null)
		    return false;
	    } else if (!s3BucketName.equals(other.s3BucketName))
		return false;
	    if (s3BucketSize == null) {
		if (other.s3BucketSize != null)
		    return false;
	    } else if (!s3BucketSize.equals(other.s3BucketSize))
		return false;
	    if (s3userId == null) {
		if (other.s3userId != null)
		    return false;
	    } else if (!s3userId.equals(other.s3userId))
		return false;
	    if (timestampMs == null) {
		if (other.timestampMs != null)
		    return false;
	    } else if (!timestampMs.equals(other.timestampMs))
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "ReportingS3BucketDeleteEvent [s3BucketName=" + s3BucketName
		    + ", s3BucketSize=" + s3BucketSize + ", s3userId="
		    + s3userId + ", timestampMs=" + timestampMs + "]";
	}
	
}
