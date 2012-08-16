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

public class ReportingVolumeSnapshotEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingVolumeSnapshotEventStore.class );

	private static ReportingVolumeSnapshotEventStore instance = null;
	
	public static synchronized ReportingVolumeSnapshotEventStore getInstance()
	{
		if (instance == null) {
			instance = new ReportingVolumeSnapshotEventStore();
		}
		return instance;
	}
	
	private ReportingVolumeSnapshotEventStore()
	{
		
	}

	public void insertCreateEvent(String uuid, String volumeSnapshotUuid, String volumeId,
			Long timestampMs, String userId, Long sizeGB)
	{
		
		EntityWrapper<ReportingVolumeSnapshotCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingVolumeSnapshotCreateEvent.class);

		try {
			ReportingVolumeSnapshotCreateEvent volumeSnapshot = new ReportingVolumeSnapshotCreateEvent(uuid,
					volumeSnapshotUuid, volumeId, timestampMs, userId, sizeGB);
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

