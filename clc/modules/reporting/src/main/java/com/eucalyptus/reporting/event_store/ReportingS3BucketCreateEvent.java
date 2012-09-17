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
import javax.persistence.*;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@SqlResultSetMapping(name="s3BucketCreateEventMap",
        entities=@EntityResult(entityClass=ReportingS3BucketCreateEvent.class))
@NamedNativeQuery(name="scanS3BucketCreateEvents",
     query="select * from reporting_s3_bucket_create_events order by timestamp_ms",
     resultSetMapping="s3BucketCreateEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_bucket_create_events")
public class ReportingS3BucketCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="user_id", nullable=false)
	protected String userId;
        
	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	protected ReportingS3BucketCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3BucketCrud class.
 	 */
	ReportingS3BucketCreateEvent(String s3BucketName, String userId, 
			Long timeInMs)
	{
		this.s3BucketName = s3BucketName;
		this.userId = userId;
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

	@Override
	public EventDependency asDependency() {
		return asDependency( "s3BucketName", s3BucketName );
	}

  @Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.set();
	}

	@Override
	public String toString() {
	    return "ReportingS3BucketCreateEvent [s3BucketName=" + s3BucketName
		    + ", timestampMs=" + timestampMs + ", userId=" + userId
		    + "]";
	}

	
}
