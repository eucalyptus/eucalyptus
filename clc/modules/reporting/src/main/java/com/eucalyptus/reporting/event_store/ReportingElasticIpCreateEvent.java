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
