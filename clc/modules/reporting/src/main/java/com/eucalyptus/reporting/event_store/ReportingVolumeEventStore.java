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

	
	public void insertAttachEvent(String uuid, String instanceUuid, long timestampMs)
	{
		EntityWrapper<ReportingVolumeAttachEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeAttachEvent.class);

		try {
			ReportingVolumeAttachEvent event = new ReportingVolumeAttachEvent(uuid, instanceUuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}							
		
	}
	
	public void insertDetachEvent(String uuid, String instanceUuid, long timestampMs)
	{
		EntityWrapper<ReportingVolumeDetachEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeDetachEvent.class);

		try {
			ReportingVolumeDetachEvent event = new ReportingVolumeDetachEvent(uuid, instanceUuid, timestampMs);
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

