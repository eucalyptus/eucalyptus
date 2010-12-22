package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="ebs_volume_create")
public class EbsVolumeCreateEvent
	implements EbsEvent
{
	@Id
	@Column(name="volume_id")
	private Long volumeId;
	@Column(name="name")
	private String name;
	@Column(name="owner")
	private String owner;
	@Column(name="size_gb")
	private Long sizeGb;
	@Column(name="created_time_ms")
	private Long createdTimeMs;
	@Column(name="zone")
	private String zone;
	@Column(name="snapshot_id")
	private Long snapshotId;
	
	public EbsVolumeCreateEvent()
	{
	}

	public Long getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(Long volumeId) {
		this.volumeId = volumeId;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOwner()
	{
		return this.owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public Long getSizeGb()
	{
		return this.sizeGb;
	}

	public void setSizeGb(Long size)
	{
		this.sizeGb = size;
	}

	public Long getCreatedTimeMs()
	{
		return this.createdTimeMs;
	}

	public void setCreatedTimeMs(Long createdTimeMs)
	{
		this.createdTimeMs = createdTimeMs;
	}

	public String getZone()
	{
		return zone;
	}

	public void setZone(String zone)
	{
		this.zone = zone;
	}

	public boolean requiresReliableTransmission()
	{
		return true;
	}

	public Long getSnapshotId()
	{
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId)
	{
		this.snapshotId = snapshotId;
	}

}
