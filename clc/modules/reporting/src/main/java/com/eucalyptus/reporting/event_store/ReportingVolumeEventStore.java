package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * @author tom.werges
 */
public class ReportingVolumeEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingVolumeEventStore.class );

	private static ReportingVolumeEventStore instance = null;
	
	public static synchronized ReportingVolumeEventStore getVolume()
	{
		if (instance == null) {
			instance = new ReportingVolumeEventStore();
		}
		return instance;
	}
	
	private ReportingVolumeEventStore()
	{
		
	}

	public void insertCreateEvent(String uuid, String volumeId, long timestampMs, String userId,
					String clusterName, String availabilityZone, Long sizeGB)
	{
		
		EntityWrapper<ReportingVolumeCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeCreateEvent.class);

		try {
			ReportingVolumeCreateEvent volume = new ReportingVolumeCreateEvent(uuid, volumeId,
				timestampMs, userId, clusterName, availabilityZone, sizeGB);
			entityWrapper.add(volume);
			entityWrapper.commit();
			LOG.debug("Added reporting volume to db:" + volume);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertDeleteEvent(String uuid, long timestampMs)
	{
		
		EntityWrapper<ReportingVolumeDeleteEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeDeleteEvent.class);

		try {
			ReportingVolumeDeleteEvent event = new ReportingVolumeDeleteEvent(uuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertUsageEvent(String uuid, long timestampMs, Long cumulativeMegsRead,
			Long cumulativeMegsWritten)
	{
		EntityWrapper<ReportingVolumeUsageEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeUsageEvent.class);

		try {
			ReportingVolumeUsageEvent event = new ReportingVolumeUsageEvent(uuid, timestampMs,
					cumulativeMegsRead, cumulativeMegsWritten);
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

