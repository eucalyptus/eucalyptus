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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_detach_events")
public class ReportingVolumeDetachEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="volume_uuid", nullable=false)
	private String volumeUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;
	
	ReportingVolumeDetachEvent()
	{
	}
	
	ReportingVolumeDetachEvent(String volumeUuid, String instanceUuid,
			long timestampMs)
	{
		this.volumeUuid = volumeUuid;
		this.instanceUuid = instanceUuid;
		this.timestampMs = timestampMs;
	}

	public String getVolumeUuid()
	{
		return volumeUuid;
	}

	public String getInstanceUuid()
	{
		return instanceUuid;
	}

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingVolumeCreateEvent.class, "uuid", volumeUuid )
				.relation( ReportingInstanceCreateEvent.class, "uuid", instanceUuid )
				.set();
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((instanceUuid == null) ? 0 : instanceUuid.hashCode());
	    result = prime * result
		    + ((timestampMs == null) ? 0 : timestampMs.hashCode());
	    result = prime * result
		    + ((volumeUuid == null) ? 0 : volumeUuid.hashCode());
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
	    ReportingVolumeDetachEvent other = (ReportingVolumeDetachEvent) obj;
	    if (instanceUuid == null) {
		if (other.instanceUuid != null)
		    return false;
	    } else if (!instanceUuid.equals(other.instanceUuid))
		return false;
	    if (timestampMs == null) {
		if (other.timestampMs != null)
		    return false;
	    } else if (!timestampMs.equals(other.timestampMs))
		return false;
	    if (volumeUuid == null) {
		if (other.volumeUuid != null)
		    return false;
	    } else if (!volumeUuid.equals(other.volumeUuid))
		return false;
	    return true;
	}


}
