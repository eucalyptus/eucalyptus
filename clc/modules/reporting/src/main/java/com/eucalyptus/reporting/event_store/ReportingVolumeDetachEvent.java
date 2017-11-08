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
