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
@Table(name="reporting_elastic_ip_create_events")
public class ReportingElasticIpCreateEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="ip", nullable=false)
	private String ip;
	@Column(name="user_id", nullable=false)
	private String userId;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingElasticIpCrud class.
 	 */
	protected ReportingElasticIpCreateEvent()
	{
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingElasticIpCrud class.
 	 */
	ReportingElasticIpCreateEvent(Long timestampMs, String ip, String userId)
	{
		this.timestampMs = timestampMs;
		this.ip = ip;
		this.userId = userId;
	}

	public String getIp()
	{
		return this.ip;
	}

	public String getUserId()
	{
		return this.userId;
	}

	@Override
	public EventDependency asDependency() {
		return asDependency( "ip", ip );
	}

  @Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.user( userId )
				.set();
	}

	@Override
	public String toString()
	{
		return "[timestampMs: " + this.timestampMs + " ip:" + this.ip + " userId:" + this.userId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		ReportingElasticIpCreateEvent other = (ReportingElasticIpCreateEvent) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
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

	

}
