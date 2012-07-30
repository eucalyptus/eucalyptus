/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

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
