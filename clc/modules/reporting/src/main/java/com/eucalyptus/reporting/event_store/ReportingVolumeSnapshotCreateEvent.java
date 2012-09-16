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
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_snapshot_create_events")
public class ReportingVolumeSnapshotCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="uuid", nullable=false, unique=true)
	private String uuid;
	@Column(name="volume_uuid", nullable=false)
	private String volumeUuid;
	@Column(name="volume_snapshot_id", nullable=false)
	private String volumeSnapshotId;
	@Column(name="user_id", nullable=false)
	private String userId;
	@Column(name="sizeGB", nullable=false)
	private Long sizeGB;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeSnapshotCrud class.
 	 */
	protected ReportingVolumeSnapshotCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeSnapshotCrud class.
 	 */
	ReportingVolumeSnapshotCreateEvent(String uuid, String volumeUuid, 
			String volumeSnapshotId, Long timestampMs, String userId, Long sizeGB)
	{
		this.uuid = uuid;
		this.volumeUuid = volumeUuid;
		this.volumeSnapshotId = volumeSnapshotId;
		this.timestampMs = timestampMs;
		this.userId = userId;
		this.sizeGB = sizeGB;
	}

	public String getUuid()
	{
		return this.uuid;
	}

	public String getVolumeUuid()
	{
		return this.volumeUuid;
	}
	
	public String getVolumeSnapshotId()
	{
		return this.volumeSnapshotId;
	}
	
	public String getUserId()
	{
		return this.userId;
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
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
		ReportingVolumeSnapshotCreateEvent other = (ReportingVolumeSnapshotCreateEvent) obj;
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
		return "[uuid:" + this.uuid + " volumeSnapshotId:" + this.volumeSnapshotId + " timestampMs:" + timestampMs + " userId:" + this.userId + "]";
	}

}
