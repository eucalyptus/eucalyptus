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
@SqlResultSetMapping(name="createVolumeEventMap",
        entities=@EntityResult(entityClass=ReportingVolumeCreateEvent.class))
@NamedNativeQuery(name="scanVolumeCreateEvents",
     query="select * from reporting_volume_create_events order by timestamp_ms",
     resultSetMapping="createVolumeEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_create_events")
public class ReportingVolumeCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="uuid", nullable=false, unique=true)
	private String uuid;
	@Column(name="volume_id", nullable=false)
	private String volumeId;
	@Column(name="user_id", nullable=false)
	private String userId;
	@Column(name="availability_zone", nullable=false)
	private String availabilityZone;
	@Column(name="sizeGB", nullable=false)
	private Long sizeGB;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeCrud class.
 	 */
	protected ReportingVolumeCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeCrud class.
 	 */
	ReportingVolumeCreateEvent(String uuid, String volumeId, long timestampMs,
				String userId, String availabilityZone, long sizeGB)
	{
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.volumeId = volumeId;
		this.userId = userId;
		this.availabilityZone = availabilityZone;
		this.sizeGB = sizeGB;
	}

	public String getUuid()
	{
		return this.uuid;
	}
	
	void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public String getVolumeId()
	{
		return this.volumeId;
	}

	public String getUserId()
	{
		return this.userId;
	}

	public String getAvailabilityZone()
	{
		return this.availabilityZone;
	}

	public Long getSizeGB()
	{
		return this.sizeGB;
	}

	@Override
	public EventDependency asDependency() {
		return asDependency( "uuid", uuid );
	}

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.set();
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
		ReportingVolumeCreateEvent other = (ReportingVolumeCreateEvent) obj;
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
		return "[uuid:" + this.uuid+ " volumeId:" + this.volumeId + " userId:" + this.userId + "]";
	}


}
