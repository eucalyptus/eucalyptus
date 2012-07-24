package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * @author tom.werges
 */
public class ReportingInstanceEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingInstanceEventStore.class );

	private static ReportingInstanceEventStore instance = null;
	
	public static synchronized ReportingInstanceEventStore getInstance()
	{
		if (instance == null) {
			instance = new ReportingInstanceEventStore();
		}
		return instance;
	}
	
	private ReportingInstanceEventStore()
	{
		
	}

	public void insertCreateEvent(String uuid, long timestampMs, String instanceId,
			String instanceType, String userId, String clusterName, String availabilityZone)
	{		
		EntityWrapper<ReportingInstanceCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingInstanceCreateEvent.class);

		try {
			ReportingInstanceCreateEvent event = new ReportingInstanceCreateEvent(uuid, timestampMs,
					instanceId, instanceType, userId, clusterName, availabilityZone);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertUsageEvent(String uuid, long timestampMs, Long cumulativeNetIoMegs,
			Long cumulativeDiskIoMegs, Integer cpuUtilizationPercent)
	{
		EntityWrapper<ReportingInstanceUsageEvent> entityWrapper =
			EntityWrapper.get(ReportingInstanceUsageEvent.class);

		try {
			ReportingInstanceUsageEvent event = new ReportingInstanceUsageEvent(uuid, timestampMs,
					cumulativeNetIoMegs, cumulativeDiskIoMegs, cpuUtilizationPercent);
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
