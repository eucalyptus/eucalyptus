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

public class ReportingInstanceEventStore extends EventStoreSupport {
  private static final ReportingInstanceEventStore instance = new ReportingInstanceEventStore( );

  public static ReportingInstanceEventStore getInstance( ) {
    return instance;
  }

  protected ReportingInstanceEventStore( ) { }

  public void insertCreateEvent( @Nonnull final String uuid,
                                          final long timestampMs,
                                 @Nonnull final String instanceId,
                                 @Nonnull final String instanceType,
                                 @Nonnull final String userId,
                                 @Nonnull final String clusterName,
                                 @Nonnull final String availabilityZone ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( instanceId, "InstanceId is required" );
    Preconditions.checkNotNull( instanceType, "InstanceType is required" );
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( clusterName, "ClusterName is required" );
    Preconditions.checkNotNull( availabilityZone, "AvailabilityZone is required" );

    persist(
        new ReportingInstanceCreateEvent(
            uuid,
            timestampMs,
            instanceId,
            instanceType,
            userId,
            clusterName,
            availabilityZone ) );
  }

  public void insertUsageEvent( @Nonnull final String uuid,
                                         final long timestampMs,
                                @Nonnull final Long cumulativeDiskIoMegs,
                                @Nonnull final Integer cpuUtilizationPercent,
                                @Nonnull final Long cumulativeNetIncomingMegsBetweenZones,
                                @Nonnull final Long cumulativeNetIncomingMegsWithinZone,
                                @Nonnull final Long cumulativeNetIncomingMegsPublicIp,
                                @Nonnull final Long cumulativeNetOutgoingMegsBetweenZones,
                                @Nonnull final Long cumulativeNetOutgoingMegsWithinZone,
                                @Nonnull final Long cumulativeNetOutgoingMegsPublicIp ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( cumulativeDiskIoMegs, "CumulativeDiskIoMegs is required" );
    Preconditions.checkNotNull( cpuUtilizationPercent, "CpuUtilizationPercent is required" );
    Preconditions.checkNotNull( cumulativeNetIncomingMegsBetweenZones, "CumulativeNetIncomingMegsBetweenZones is required" );
    Preconditions.checkNotNull( cumulativeNetIncomingMegsWithinZone, "CumulativeNetIncomingMegsWithinZone is required" );
    Preconditions.checkNotNull( cumulativeNetIncomingMegsPublicIp, "CumulativeNetIncomingMegsPublicIp is required" );
    Preconditions.checkNotNull( cumulativeNetOutgoingMegsBetweenZones, "CumulativeNetOutgoingMegsBetweenZones is required" );
    Preconditions.checkNotNull( cumulativeNetOutgoingMegsWithinZone, "CumulativeNetOutgoingMegsWithinZone is required" );
    Preconditions.checkNotNull( cumulativeNetOutgoingMegsPublicIp, "CumulativeNetOutgoingMegsPublicIp is required" );

    persist(
        new ReportingInstanceUsageEvent(
            uuid,
            timestampMs,
            cumulativeDiskIoMegs,
            cpuUtilizationPercent,
            cumulativeNetIncomingMegsBetweenZones,
            cumulativeNetIncomingMegsWithinZone,
            cumulativeNetIncomingMegsPublicIp,
            cumulativeNetOutgoingMegsBetweenZones,
            cumulativeNetOutgoingMegsWithinZone,
            cumulativeNetOutgoingMegsPublicIp ) );
  }
}
