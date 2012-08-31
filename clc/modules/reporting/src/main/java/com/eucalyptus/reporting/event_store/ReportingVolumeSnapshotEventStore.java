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

import com.google.common.base.Preconditions;

public class ReportingVolumeSnapshotEventStore extends EventStoreSupport
{
  private static final ReportingVolumeSnapshotEventStore instance = new ReportingVolumeSnapshotEventStore();

  public static ReportingVolumeSnapshotEventStore getInstance() {
    return instance;
  }

  protected ReportingVolumeSnapshotEventStore( ) {
  }

  public void insertCreateEvent( final String uuid,
                                 final String volumeSnapshotId,
                                 final long timestampMs,
                                 final String userId,
                                 final long sizeGB ) {
    Preconditions.checkNotNull(uuid, "Uuid is required");
    Preconditions.checkNotNull(volumeSnapshotId, "VolumeSnapshotId is required");
    Preconditions.checkNotNull(userId, "UserId is required");

    persist( new ReportingVolumeSnapshotCreateEvent(uuid, volumeSnapshotId, timestampMs, userId, sizeGB ) );
  }

  public void insertDeleteEvent( final String uuid,
                                 final long timestampMs )
  {
    Preconditions.checkNotNull(uuid, "Uuid is required");

    persist( new ReportingVolumeSnapshotDeleteEvent(uuid, timestampMs) );
  }
}

