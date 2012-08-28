package com.eucalyptus.reporting.event_store;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_attach_events")
public class ReportingVolumeAttachEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="volume_uuid", nullable=false)
	private String volumeUuid;
	@Column(name="instance_uuid", nullable=false)
	private String instanceUuid;
	@Column(name = "size_gb", nullable = false)
	private Long sizeGB;

	
	ReportingVolumeAttachEvent()
	{
	}
	
	ReportingVolumeAttachEvent(String volumeUuid, String instanceUuid,
			long sizeGB, long timestampMs)
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
	    ReportingVolumeAttachEvent other = (ReportingVolumeAttachEvent) obj;
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
