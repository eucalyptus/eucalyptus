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

import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;

public class ReportingElasticIpEventStore extends EventStoreSupport {
  private static final ReportingElasticIpEventStore instance = new ReportingElasticIpEventStore();

  public static ReportingElasticIpEventStore getInstance() {
    return instance;
  }

  protected ReportingElasticIpEventStore() {
  }

  public void insertCreateEvent(          final long timestampMs,
                                 @Nonnull final String userId,
                                 @Nonnull final String ip ) {
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( ip, "IP is required" );

    persist( new ReportingElasticIpCreateEvent(timestampMs, ip, userId) );
  }

  public void insertDeleteEvent( @Nonnull final String ip,
                                          final long timestampMs )
  {
    Preconditions.checkNotNull( ip, "IP is required" );

    persist( new ReportingElasticIpDeleteEvent(ip, timestampMs) );
  }

  public void insertAttachEvent( @Nonnull final String ip,
                                 @Nonnull final String instanceUuid,
                                          final long timestampMs ) {
    Preconditions.checkNotNull( ip, "IP is required" );
    Preconditions.checkNotNull( instanceUuid, "InstanceUuid is required" );

    persist( new ReportingElasticIpAttachEvent( ip, instanceUuid, timestampMs) );
  }

  public void insertDetachEvent( @Nonnull final String ip,
                                 @Nonnull final String instanceUuid,
                                          final long timestampMs ) {
    Preconditions.checkNotNull( ip, "IP is required" );
    Preconditions.checkNotNull( instanceUuid, "InstanceUuid is required" );

    persist( new ReportingElasticIpDetachEvent( ip, instanceUuid, timestampMs ) );
  }
}

