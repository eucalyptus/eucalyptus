/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.event_store;

import com.google.common.base.Preconditions;

public class ReportingVolumeEventStore extends EventStoreSupport
{
  private static final ReportingVolumeEventStore instance = new ReportingVolumeEventStore();

  public static ReportingVolumeEventStore getInstance() {
    return instance;
  }

  protected ReportingVolumeEventStore() {
  }

  public void insertCreateEvent( final String uuid,
                                 final String volumeId,
                                 final long timestampMs,
                                 final String userId,
                                 final String availabilityZone,
                                 final long sizeGB ) {
    Preconditions.checkNotNull(uuid, "Uuid is required");
    Preconditions.checkNotNull(volumeId, "VolumeId is required");
    Preconditions.checkNotNull(userId, "UserId is required");
    Preconditions.checkNotNull(availabilityZone, "AvailabilityZone is required");

    persist( new ReportingVolumeCreateEvent(uuid, volumeId, timestampMs, userId, availabilityZone, sizeGB) );
  }

  public void insertDeleteEvent( final String uuid,
                                 final long timestampMs) {

    Preconditions.checkNotNull(uuid, "Uuid is required");

    persist( new ReportingVolumeDeleteEvent(uuid, timestampMs) );
  }

  public void insertAttachEvent( final String uuid,
                                 final String instanceUuid,
                                 final long sizeGB,
                                 final long timestampMs) {
    Preconditions.checkNotNull(uuid, "Uuid is required");
    Preconditions.checkNotNull(instanceUuid, "InstanceUuid is required");

    persist( new ReportingVolumeAttachEvent(uuid, instanceUuid, sizeGB, timestampMs) );
  }

  public void insertDetachEvent( final String uuid,
                                 final String instanceUuid,
                                 final long timestampMs ) {
    Preconditions.checkNotNull(uuid, "Uuid is required");
    Preconditions.checkNotNull(instanceUuid, "InstanceUuid is required");

    persist( new ReportingVolumeDetachEvent(uuid, instanceUuid, timestampMs) );
  }

}

