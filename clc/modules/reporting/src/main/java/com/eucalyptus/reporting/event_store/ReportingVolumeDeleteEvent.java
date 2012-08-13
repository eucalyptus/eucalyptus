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

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_delete_events")
public class ReportingVolumeDeleteEvent
	extends AbstractPersistent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
		
	protected ReportingVolumeDeleteEvent(String uuid, Long timestampMs)
	{
		super();
		this.uuid = uuid;
		this.timestampMs = timestampMs;
	}

	protected ReportingVolumeDeleteEvent()
	{
		super();
		this.uuid = null;
		this.timestampMs = null;
	}

	public String getUuid()
	{
		return uuid;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportingVolumeDeleteEvent other = (ReportingVolumeDeleteEvent) obj;
		if (timestampMs == null) {
			if (other.timestampMs != null)
				return false;
		} else if (!timestampMs.equals(other.timestampMs))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	

}
