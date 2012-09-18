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

@Entity
@javax.persistence.Entity
@PersistenceContext(name = "eucalyptus_reporting")
@Table(name = "reporting_s3_object_usage_events")
public class ReportingS3ObjectUsageEvent
	extends ReportingEventSupport
{
    private static final long serialVersionUID = 1L;

    @Column(name = "bucket_name", nullable = false)
    protected String bucketName;
    @Column(name = "object_name", nullable = false)
    protected String objectName;
	@Column(name="object_version", nullable=true) //version can be null as per disc with Zach
	protected String objectVersion;
    @Column(name = "user_id", nullable = false)
    protected String userId;
	@Column(name="get_requests_num_cumulative", nullable=false)
	protected Long getRequestsNumCumulative;

    protected ReportingS3ObjectUsageEvent() {
    }

    ReportingS3ObjectUsageEvent(String s3BucketName, String s3ObjectName,
	    String objectVersion, long getRequestsNumCumulative,
	    long timestampMs, String userId)
	{
    	this.bucketName = s3BucketName;
    	this.objectName = s3ObjectName;
    	this.objectVersion = objectVersion;
    	this.timestampMs = timestampMs;
    	this.getRequestsNumCumulative = getRequestsNumCumulative;
    	this.userId = userId;
    }

    public String getBucketName()
    {
    	return bucketName;
    }

    public String getObjectName()
    {
    	return objectName;
    }

	public Long getGetRequestsNumCumulative()
	{
		return getRequestsNumCumulative;
	}
	
	public String getObjectVersion()
	{
		return objectVersion;
	}

    public String getUserId()
    {
    	return userId;
    }

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.relation( ReportingS3ObjectCreateEvent.class, "s3ObjectName", objectName )
				.set();
	}

    @Override
    public String toString() {
	return "ReportingS3ObjectUsageEvent [bucketName=" + bucketName
		+ ", objectName=" + objectName
		+ ", timestampMs=" + timestampMs + ", userId=" + userId + "]";
    }

}
