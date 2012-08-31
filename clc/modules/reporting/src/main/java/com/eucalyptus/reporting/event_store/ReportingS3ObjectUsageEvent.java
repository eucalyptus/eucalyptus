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


@Entity
@javax.persistence.Entity
@PersistenceContext(name = "eucalyptus_reporting")
@Table(name = "reporting_s3_object_usage_events")
public class ReportingS3ObjectUsageEvent extends AbstractPersistent {
    @Column(name = "bucket_name", nullable = false)
    protected String bucketName;
    @Column(name = "object_name", nullable = false)
    protected String objectName;
    @Column(name = "object_size", nullable = false)
    protected Long object_size;
    @Column(name = "timestamp_ms", nullable = false)
    protected Long timestampMs;
    @Column(name = "user_id", nullable = false)
    protected String userId;

    protected ReportingS3ObjectUsageEvent() {
	this.bucketName = null;
	this.objectName = null;
	this.object_size = null;
	this.timestampMs = null;
	this.userId = null;
    }

    ReportingS3ObjectUsageEvent(String s3BucketName, String s3ObjectName,
	    Long s3ObjectSize, Long timestampMs, String userId) {
	this.bucketName = s3BucketName;
	this.objectName = s3ObjectName;
	this.object_size = s3ObjectSize;
	this.timestampMs = timestampMs;
	this.userId = userId;
    }

    public String getBucketName() {
	return bucketName;
    }

    public String getObjectName() {
	return objectName;
    }

    public Long getObject_size() {
	return object_size;
    }

    public Long getTimestampMs() {
	return timestampMs;
    }

    public String getUserId() {
	return userId;
    }

    @Override
    public String toString() {
	return "ReportingS3ObjectUsageEvent [bucketName=" + bucketName
		+ ", objectName=" + objectName + ", object_size=" + object_size
		+ ", timestampMs=" + timestampMs + ", userId=" + userId + "]";
    }

}
