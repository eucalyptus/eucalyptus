package com.eucalyptus.reporting.event;

import javax.persistence.*;

public class EbsSnapshotCreateEvent
	implements Event
{
    @Id
    @Column(name = "snapshot_id")
    private Long snapshotId;
    @Column(name = "owner")
    private String owner;
    @Column(name = "sc_name")
    private String scName;
    @Column(name = "name")
    private String name;
    @Column(name = "volume_id")
    private Long volumeId;
    @Column(name="creation_time_ms")
    private Long creationMs;
    
    public EbsSnapshotCreateEvent()
    {	
    }

	public Long getSnapshotId()
	{
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId)
	{
		this.snapshotId = snapshotId;
	}

	public String getOwner()
	{
		return owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public String getScName()
	{
		return scName;
	}

	public void setScName(String scName)
	{
		this.scName = scName;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Long getVolumeId()
	{
		return volumeId;
	}

	public void setVolumeId(Long volumeId)
	{
		this.volumeId = volumeId;
	}

	public Long getCreationMs()
	{
		return creationMs;
	}

	public void setCreationMs(Long creationMs)
	{
		this.creationMs = creationMs;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}
    
}
