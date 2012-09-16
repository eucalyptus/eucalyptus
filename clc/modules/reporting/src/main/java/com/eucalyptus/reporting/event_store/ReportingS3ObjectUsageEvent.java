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
public class ReportingS3ObjectUsageEvent extends ReportingEventSupport {
    private static final long serialVersionUID = 1L;

    @Column(name = "bucket_name", nullable = false)
    protected String bucketName;
    @Column(name = "object_name", nullable = false)
    protected String objectName;
    @Column(name = "user_id", nullable = false)
    protected String userId;
	@Column(name="size_gb", nullable=false)
	protected Long sizeGB;
	@Column(name="get_requests_num", nullable=false)
	protected Long getRequestsNum;
	@Column(name="put_requests_num", nullable=false)
	protected Long putRequestsNum;
	@Column(name="data_in_gb", nullable=false)
	protected Long dataInGB;
	@Column(name="data_out_gb", nullable=false)
	protected Long dataOutGB;

    protected ReportingS3ObjectUsageEvent() {
    }

    ReportingS3ObjectUsageEvent(String s3BucketName, String s3ObjectName,
	    Long s3ObjectSize, Long timestampMs, String userId)
	{
    	this.bucketName = s3BucketName;
    	this.objectName = s3ObjectName;
    	this.timestampMs = timestampMs;
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

	public Long getGetRequestsNum()
	{
		return getRequestsNum;
	}

	public void setGetRequestsNum(Long getRequestsNum)
	{
		this.getRequestsNum = getRequestsNum;
	}

	public Long getPutRequestsNum()
	{
		return putRequestsNum;
	}

	public void setPutRequestsNum(Long putRequestsNum)
	{
		this.putRequestsNum = putRequestsNum;
	}

	public Long getDataInGB()
	{
		return dataInGB;
	}

	public void setDataInGB(Long dataInGB)
	{
		this.dataInGB = dataInGB;
	}

	public Long getDataOutGB()
	{
		return dataOutGB;
	}

    public String getUserId()
    {
    	return userId;
    }

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.relation( ReportingS3BucketCreateEvent.class, "s3BucketName", bucketName )
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
