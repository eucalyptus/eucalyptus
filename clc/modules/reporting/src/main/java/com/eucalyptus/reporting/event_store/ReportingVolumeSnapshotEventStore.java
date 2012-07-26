package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * @author tom.werges
 */
public class ReportingVolumeSnapshotEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingVolumeSnapshotEventStore.class );

	private static ReportingVolumeSnapshotEventStore instance = null;
	
	public static synchronized ReportingVolumeSnapshotEventStore getVolumeSnapshot()
	{
		if (instance == null) {
			instance = new ReportingVolumeSnapshotEventStore();
		}
		return instance;
	}
	
	private ReportingVolumeSnapshotEventStore()
	{
		
	}

	public void insertCreateEvent(String uuid, String volumeSnapshotId, String volumeId,
			Long timestampMs, String userId, Long sizeGB)
	{
		
		EntityWrapper<ReportingVolumeSnapshotCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeSnapshotCreateEvent.class);

		try {
			ReportingVolumeSnapshotCreateEvent volumeSnapshot = new ReportingVolumeSnapshotCreateEvent(uuid,
					volumeSnapshotId, volumeId, timestampMs, userId, sizeGB);
			entityWrapper.add(volumeSnapshot);
			entityWrapper.commit();
			LOG.debug("Added reporting volumeSnapshot to db:" + volumeSnapshot);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}
	
	public void insertDeleteEvent(String uuid, long timestampMs)
	{
		
		EntityWrapper<ReportingVolumeSnapshotDeleteEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeSnapshotDeleteEvent.class);

		try {
			ReportingVolumeSnapshotDeleteEvent event = new ReportingVolumeSnapshotDeleteEvent(uuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}


}

