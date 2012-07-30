package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_elastic_ip_create_events")
public class ReportingElasticIpCreateEvent
	extends AbstractPersistent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	@Column(name="ip", nullable=false)
	private String ip;
	@Column(name="user_id", nullable=false)
	private String userId;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingElasticIpCrud class.
 	 */
	protected ReportingElasticIpCreateEvent()
	{
		//NOTE: hibernate will overwrite these
		this.uuid = null;
		this.timestampMs = null;
		this.ip = null;
		this.userId = null;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingElasticIpCrud class.
 	 */
	ReportingElasticIpCreateEvent(String uuid, Long timestampMs, String ip, String userId)
	{
		this.uuid = null;
		this.timestampMs = timestampMs;
		this.ip = ip;
		this.userId = userId;
	}

	public String getUuid()
	{
		return this.uuid;
	}
	
	public Long getTimestampMs()
	{
		return this.timestampMs;
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
	public String toString()
	{
		return "[uuid:" + this.uuid + " timestampMs: " + this.timestampMs + " ip:" + this.ip + " userId:" + this.userId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	

}
