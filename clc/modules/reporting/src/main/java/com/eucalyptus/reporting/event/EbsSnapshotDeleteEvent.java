package com.eucalyptus.reporting.event;

import javax.persistence.Column;

public class EbsSnapshotDeleteEvent
	implements Event
{
    @Column(name = "snapshot_id")
    private Long snapshotId;
    @Column(name="deletion_time_ms")
    private Long deletionMs;

        
	public Long getSnapshotId()
	{
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId)
	{
		this.snapshotId = snapshotId;
	}

	public Long getDeletionMs()
	{
		return deletionMs;
	}

	public void setDeletionMs(Long deletionMs)
	{
		this.deletionMs = deletionMs;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}

}
