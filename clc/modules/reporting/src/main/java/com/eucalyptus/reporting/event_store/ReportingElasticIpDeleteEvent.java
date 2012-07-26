package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_elastic_ip_delete_events")
public class ReportingElasticIpDeleteEvent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
		
	protected ReportingElasticIpDeleteEvent(String uuid, Long timestampMs)
	{
		super();
		this.uuid = uuid;
		this.timestampMs = timestampMs;
	}

	protected ReportingElasticIpDeleteEvent()
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
		ReportingElasticIpDeleteEvent other = (ReportingElasticIpDeleteEvent) obj;
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
