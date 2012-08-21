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
@Table(name="reporting_instance_attribute_events")
public class ReportingInstanceAttributeEvent
	extends AbstractPersistent
{
	@Column(name="uuid",unique=true)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	@Column(name="instance_id", nullable=false)
	private String instanceId;
	@Column(name="instance_type")
	private String instanceType;
	@Column(name="user_id")
	private String userId;
	@Column(name="cluster_name")
	private String clusterName;
	@Column(name="availability_zone")
	private String availabilityZone;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingInstanceEventStore class.
 	 */
	protected ReportingInstanceAttributeEvent()
	{
		//NOTE: hibernate will overwrite these
		this.uuid = null;
		this.timestampMs = null;
		this.instanceId = null;
		this.instanceType = null;
		this.userId = null;
		this.clusterName = null;
		this.availabilityZone = null;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingInstanceEventStore class.
 	 */
	ReportingInstanceAttributeEvent(String uuid, Long timestampMs, String instanceId, String instanceType,
				String userId, String clusterName, String availabilityZone)
	{
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
	}

	public String getUuid()
	{
		return this.uuid;
	}
	
	void setUuid(String uuid)
	{
		this.uuid = uuid;
	}
	
	public Long getTimestampMs()
	{
		return this.timestampMs;
	}

	public String getInstanceId()
	{
		return this.instanceId;
	}

	public String getInstanceType()
	{
		return this.instanceType;
	}

	public String getUserId()
	{
		return this.userId;
	}
	
	public String getClusterName()
	{
		return this.clusterName;
	}

	public String getAvailabilityZone()
	{
		return this.availabilityZone;
	}

	@Override
	public int hashCode()
	{
		return (uuid == null) ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		ReportingInstanceAttributeEvent other = (ReportingInstanceAttributeEvent) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "[uuid:" + this.uuid+ " instanceId:" + this.instanceId + " userId:" + this.userId + "]";
	}


}
