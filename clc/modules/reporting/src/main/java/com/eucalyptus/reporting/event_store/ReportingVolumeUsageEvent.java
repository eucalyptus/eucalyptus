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
@Table(name="reporting_volume_usage_events")
public class ReportingVolumeUsageEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="cumulative_megs_read", nullable=true)
	protected Long cumulativeMegsRead;
	@Column(name="cumulative_megs_written", nullable=true)
	protected Long cumulativeMegsWritten;

	protected ReportingVolumeUsageEvent()
	{
	}

	ReportingVolumeUsageEvent(String uuid, Long timestampMs,
			Long cumulativeMegsRead, Long cumulativeMegsWritten)
	{
		if (timestampMs == null)
			throw new IllegalArgumentException("timestampMs can't be null");
		if (uuid == null)
			throw new IllegalArgumentException("volumeId can't be null");
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.cumulativeMegsRead = cumulativeMegsRead;
		this.cumulativeMegsWritten = cumulativeMegsWritten;
	}

	public String getUuId()
	{
		return uuid;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeMegsRead()
	{
		return cumulativeMegsRead;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeMegsWritten()
	{
		return cumulativeMegsWritten;
	}

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingVolumeCreateEvent.class, "uuid", uuid )
				.set();
	}

	@Override
	public String toString()
	{
		return "[timestamp: " + this.timestampMs + " uuid:" + this.uuid + " cumulMegsRead:"
			+ this.cumulativeMegsRead + " cumulMegsWritten:" + this.cumulativeMegsWritten + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportingVolumeUsageEvent other = (ReportingVolumeUsageEvent) obj;
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
