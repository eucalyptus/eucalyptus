package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_detach_events")
public class ReportingVolumeDetachEvent
	extends AbstractPersistent
{
	@Column(name="volume_uuid", nullable=false)
	private String volumeUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;
	@Column(name="size_gb", nullable = false)
	private Long sizeGB;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;

	
	ReportingVolumeDetachEvent()
	{
		this.volumeUuid = null;
		this.instanceUuid = null;
		this.sizeGB = null;
		this.timestampMs = null;
	}
	
	ReportingVolumeDetachEvent(String volumeUuid, String instanceUuid,
			Long sizeGB, long timestampMs)
	{
		this.volumeUuid = volumeUuid;
		this.instanceUuid = instanceUuid;
		this.sizeGB = sizeGB;
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

	public Long getSizeGB()
	{
	    	return sizeGB;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result
		    + ((instanceUuid == null) ? 0 : instanceUuid.hashCode());
	    result = prime * result
		    + ((sizeGB == null) ? 0 : sizeGB.hashCode());
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
	    if (sizeGB == null) {
		if (other.sizeGB != null)
		    return false;
	    } else if (!sizeGB.equals(other.sizeGB))
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
