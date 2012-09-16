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

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_object_delete_events")
public class ReportingS3ObjectDeleteEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="s3_bucket_name", nullable=false)
	private String s3BucketName;
	@Column(name="s3_object_name", nullable=false)
	private String s3ObjectName;

	protected ReportingS3ObjectDeleteEvent()
	{
	}
	
	protected ReportingS3ObjectDeleteEvent(String s3BucketName, String s3ObjectName, Long timestampMs)
	{
		this.s3BucketName = s3BucketName;
		this.s3ObjectName = s3ObjectName;
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
	
	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingS3BucketCreateEvent.class, "s3BucketName", s3BucketName )
				.relation( ReportingS3ObjectCreateEvent.class, "s3ObjectName", s3ObjectName )
				.set();
	}

	@Override
	public String toString() {
	    return "ReportingS3ObjectDeleteEvent [s3BucketName=" + s3BucketName
		    + ", s3ObjectName=" + s3ObjectName + ", s3ObjectSize="
		    + ", timestampMs=" + timestampMs + "]";
	}

}
