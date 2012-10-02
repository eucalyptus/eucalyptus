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
import javax.persistence.EntityResult;
import javax.persistence.NamedNativeQuery;
import javax.persistence.PersistenceContext;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@SqlResultSetMapping(name="elasticIpAttachEventMap",
        entities=@EntityResult(entityClass=ReportingElasticIpAttachEvent.class))
@NamedNativeQuery(name="scanElasticIpAttachEvents",
     query="select * from reporting_elastic_ip_attach_events order by timestamp_ms",
     resultSetMapping="elasticIpAttachEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_elastic_ip_attach_events")
public class ReportingElasticIpAttachEvent
	extends ReportingEventSupport
{
  private static final long serialVersionUID = 1L;

  @Column(name="ip_uuid", nullable=false)
	private String ipUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;

	protected ReportingElasticIpAttachEvent(String ipUuid, String instanceUuid,
			Long timestampMs)
	{
		this.ipUuid = ipUuid;
		this.instanceUuid = instanceUuid;
		this.timestampMs = timestampMs;
	}

	protected ReportingElasticIpAttachEvent()
	{
	}

	public String getIpUuid()
	{
		return ipUuid;
	}
	
	public String getInstanceUuid()
	{
		return instanceUuid;
	}

  @Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation(ReportingElasticIpCreateEvent.class, "uuid", ipUuid)
				.set();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instanceUuid == null) ? 0 : instanceUuid.hashCode());
		result = prime * result + ((ipUuid == null) ? 0 : ipUuid.hashCode());
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
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
		ReportingElasticIpAttachEvent other = (ReportingElasticIpAttachEvent) obj;
		if (instanceUuid == null) {
			if (other.instanceUuid != null)
				return false;
		} else if (!instanceUuid.equals(other.instanceUuid))
			return false;
		if (ipUuid == null) {
			if (other.ipUuid != null)
				return false;
		} else if (!ipUuid.equals(other.ipUuid))
			return false;
		if (timestampMs == null) {
			if (other.timestampMs != null)
				return false;
		} else if (!timestampMs.equals(other.timestampMs))
			return false;
		return true;
	}


}
