package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_elastic_ip_detach_events")
public class ReportingElasticIpDetachEvent
{
	@Column(name="ip_uuid", nullable=false)
	private String ipUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	
	
	
	protected ReportingElasticIpDetachEvent(String ipUuid, String instanceUuid,
			Long timestampMs)
	{
		super();
		this.ipUuid = ipUuid;
		this.instanceUuid = instanceUuid;
		this.timestampMs = timestampMs;
	}

	protected ReportingElasticIpDetachEvent()
	{
		super();
		this.ipUuid = null;
		this.instanceUuid = null;
		this.timestampMs = null;
	}

	public String getIpUuid()
	{
		return ipUuid;
	}
	
	public String getInstanceUuid()
	{
		return instanceUuid;
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
		ReportingElasticIpDetachEvent other = (ReportingElasticIpDetachEvent) obj;
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
