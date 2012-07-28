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

	private final Set<String> alreadyHaveAttrUuids;

	private ReportingInstanceEventStore()
	{
		this.alreadyHaveAttrUuids = new HashSet<String>();	
	}

	/**
	 * This can be called repeatedly and only one event will be stored in the database.
	 */
	public void insertAttributesEvent(String uuid, long timestampMs, String instanceId,
			String instanceType, String userId, String clusterName, String availabilityZone)
	{
		EntityWrapper<ReportingInstanceAttributeEvent> entityWrapper =
			EntityWrapper.get(ReportingInstanceAttributeEvent.class);

		try {
			/* NOTE: it's possible that the CLC has rebooted and cleared out this cache of
			 * uuids, in which case the uuid would have already been inserted. The database
			 * column has a unique constraint and the subsequent insertion will fail, which is
			 * what we want in this case. There are more elegant ways of doing this which
			 * I'll explore later. -tw
			 */
			if (!alreadyHaveAttrUuids.contains(uuid)) {
				ReportingInstanceAttributeEvent event =
					new ReportingInstanceAttributeEvent(uuid, timestampMs, instanceId, instanceType,
							userId, clusterName, availabilityZone);

				entityWrapper.add(event);
				entityWrapper.commit();
				alreadyHaveAttrUuids.add(uuid);
				LOG.debug("Added event to db:" + event);
			}
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertUsageEvent(String uuid, long timestampMs, Long cumulativeDiskIoMegs,
			Integer cpuUtilizationPercent, Long cumulativeNetIncomingMegsBetweenZones,
			Long cumulateiveNetIncomingMegsWithinZone, Long cumulativeNetIncomingMegsPublicIp,
			Long cumulativeNetOutgoingMegsBetweenZones,	Long cumulateiveNetOutgoingMegsWithinZone,
			Long cumulativeNetOutgoingMegsPublicIp)
	{
		EntityWrapper<ReportingInstanceUsageEvent> entityWrapper =
			EntityWrapper.get(ReportingInstanceUsageEvent.class);

		try {
			ReportingInstanceUsageEvent event = new ReportingInstanceUsageEvent(uuid, timestampMs,
					cumulativeDiskIoMegs, cpuUtilizationPercent, cumulativeNetIncomingMegsBetweenZones,
					cumulateiveNetIncomingMegsWithinZone, cumulativeNetIncomingMegsPublicIp,
					cumulativeNetOutgoingMegsBetweenZones, cumulateiveNetOutgoingMegsWithinZone,
					cumulativeNetOutgoingMegsPublicIp);
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
