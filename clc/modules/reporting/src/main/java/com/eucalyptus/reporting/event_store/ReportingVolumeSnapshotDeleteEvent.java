package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_snapshot_delete_events")
public class ReportingVolumeSnapshotDeleteEvent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	
	protected ReportingVolumeSnapshotDeleteEvent(String uuid, Long timestampMs)
	{
		super();
		this.uuid = uuid;
		this.timestampMs = timestampMs;
	}

	protected ReportingVolumeSnapshotDeleteEvent()
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
	
	
}
