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
@SqlResultSetMapping(name="s3ObjectCreateEventMap",
        entities=@EntityResult(entityClass=ReportingS3ObjectCreateEvent.class))
@NamedNativeQuery(name="scanS3ObjectCreateEvents",
     query="select * from reporting_s3_object_create_events order by s3_bucket_name, timestamp_ms",
     resultSetMapping="s3ObjectCreateEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_s3_object_create_events")
public class ReportingS3ObjectCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="s3_bucket_name", nullable=false)
	protected String s3BucketName;
	@Column(name="s3_object_name", nullable=false)
	protected String s3ObjectKey;
	@Column(name="s3_object_version", nullable=true) //version can be null as per disc with Zach
	protected String objectVersion;
	@Column(name="size_gb", nullable=false)
	protected Long sizeGB;
	@Column(name="user_id", nullable=false)
	protected String userId;
	
	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	protected ReportingS3ObjectCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingS3ObjectCrud class.
 	 */
	ReportingS3ObjectCreateEvent(String s3BucketName, String s3ObjectName, String objectVersion,
			Long sizeGB, Long timestampMs, String userId)
	{
		this.s3BucketName = s3BucketName;
		this.s3ObjectKey = s3ObjectName;
		this.objectVersion = objectVersion;
		this.sizeGB = sizeGB;
		this.timestampMs = timestampMs;
		this.userId = userId;
	}

	public String getS3BucketName()
	{
		return this.s3BucketName;
	}

	public String getS3ObjectKey()
	{
		return this.s3ObjectKey;
	}

	public String getUserId()
	{
		return this.userId;
	}
	
	public Long getSizeGB()
	{
	    	return this.sizeGB;
	}

	public String getObjectVersion()
	{
		return objectVersion;
	}

	@Override
	public EventDependency asDependency() {
		return asDependency( "s3ObjectName", s3ObjectKey );
	}

  @Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.set();
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((s3BucketName == null) ? 0 : s3BucketName.hashCode());
	    result = prime * result
		    + ((s3ObjectKey == null) ? 0 : s3ObjectKey.hashCode());
	    result = prime * result
		    + ((sizeGB == null) ? 0 : sizeGB.hashCode());
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
	    ReportingS3ObjectCreateEvent other = (ReportingS3ObjectCreateEvent) obj;
	    if (s3BucketName == null) {
		if (other.s3BucketName != null)
		    return false;
	    } else if (!s3BucketName.equals(other.s3BucketName))
		return false;
	    if (s3ObjectKey == null) {
		if (other.s3ObjectKey != null)
		    return false;
	    } else if (!s3ObjectKey.equals(other.s3ObjectKey))
		return false;
	    if (sizeGB == null) {
		if (other.sizeGB != null)
		    return false;
	    } else if (!sizeGB.equals(other.sizeGB))
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

	@Override
	public String toString() {
	    return "ReportingS3ObjectCreateEvent [s3BucketName=" + s3BucketName
		    + ", s3ObjectName=" + s3ObjectKey + ", timestampMs="
		    + timestampMs + ", userId=" + userId + ", s3ObjectSize="
		    + sizeGB + "]";
	}


}
