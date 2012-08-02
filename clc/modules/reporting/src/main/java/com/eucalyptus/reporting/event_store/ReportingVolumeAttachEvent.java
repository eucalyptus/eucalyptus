package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_attach_events")
public class ReportingVolumeAttachEvent
	extends AbstractPersistent
{
	@Column(name="volume_uuid", nullable=false)
	private String volumeUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;

	
	ReportingVolumeAttachEvent()
	{
		this.volumeUuid = null;
		this.instanceUuid = null;
		this.timestampMs = null;
	}
	
	ReportingVolumeAttachEvent(String volumeUuid, String instanceUuid,
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportingVolumeAttachEvent other = (ReportingVolumeAttachEvent) obj;
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
