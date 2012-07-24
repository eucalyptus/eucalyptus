package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume")
public class ReportingVolumeCreateEvent
	extends AbstractPersistent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	@Column(name="volume_id", nullable=false)
	private String volumeId;
	@Column(name="user_id", nullable=false)
	private String userId;
	@Column(name="cluster_name", nullable=false)
	private String clusterName;
	@Column(name="availability_zone", nullable=false)
	private String availabilityZone;
	@Column(name="sizeGB", nullable=false)
	private Long sizeGB;


	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeCrud class.
 	 */
	protected ReportingVolumeCreateEvent()
	{
		//NOTE: hibernate will overwrite these
		this.uuid = null;
		this.timestampMs = null;
		this.volumeId = null;
		this.userId = null;
		this.clusterName = null;
		this.availabilityZone = null;
		this.sizeGB = null;
	}

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingVolumeCrud class.
 	 */
	ReportingVolumeCreateEvent(String uuid, String volumeId, Long timestampMs,
				String userId, String clusterName, String availabilityZone, Long sizeGB)
	{
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.volumeId = volumeId;
		this.userId = userId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
		this.sizeGB = sizeGB;
	}

	public String getUuid()
	{
		return this.uuid;
	}
	
	void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public String getVolumeId()
	{
		return this.volumeId;
	}

	public String getUserId()
	{
		return this.userId;
	}

	public String getClusterName()
	{
		return this.clusterName;
	}

	public String getAvailabilityZone()
	{
		return this.availabilityZone;
	}

	public Long getSizeGB()
	{
		return this.sizeGB;
	}

	@Override
	public int hashCode()
	{
		return (uuid == null) ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		ReportingVolumeCreateEvent other = (ReportingVolumeCreateEvent) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "[uuid:" + this.uuid+ " volumeId:" + this.volumeId + " userId:" + this.userId + "]";
	}

}
