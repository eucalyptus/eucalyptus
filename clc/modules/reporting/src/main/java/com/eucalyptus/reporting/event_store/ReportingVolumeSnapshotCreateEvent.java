/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.event_store;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.component.annotation.RemotablePersistence;

@Entity
@PersistenceContext(name="eucalyptus_reporting_backend")
@RemotablePersistence
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
