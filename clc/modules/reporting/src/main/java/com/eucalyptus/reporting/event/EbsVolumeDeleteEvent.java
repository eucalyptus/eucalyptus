package com.eucalyptus.reporting.event;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="ebs_volume_delete")
public class EbsVolumeDeleteEvent
	implements EbsEvent
{
	@Id
	@Column(name="volume_id")
	private Long volume_id;
	@Column(name="deleted_time_ms")
	private Long deletedTimeMs;

	public EbsVolumeDeleteEvent()
	{
	}

	
	
	public Long getVolume_id()
	{
		return volume_id;
	}

	public void setVolume_id(Long volume_id)
	{
		this.volume_id = volume_id;
	}

	public Long getDeletedTimeMs()
	{
		return this.deletedTimeMs;
	}

	public void setDeletedTimeMs(Long deletedTimeMs)
	{
		this.deletedTimeMs = deletedTimeMs;
	}

	public boolean requiresReliableTransmission()
	{
		return true;
	}

}
