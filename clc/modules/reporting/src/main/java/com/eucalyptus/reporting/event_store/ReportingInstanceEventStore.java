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
  private static final ReportingInstanceEventStore instance = new ReportingInstanceEventStore();

  public static ReportingInstanceEventStore getInstance() {
    return instance;
  }

  protected ReportingInstanceEventStore() {
  }

  public void insertCreateEvent(
      @Nonnull final String uuid,
      @Nonnull final String instanceId,
      @Nonnull final Long timestampMs,
      @Nonnull final String instanceType,
      @Nonnull final String userId,
      @Nonnull final String availabilityZone ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( instanceId, "InstanceId is required" );
    Preconditions.checkNotNull( timestampMs, "TimestampMs is required" );
    Preconditions.checkNotNull( instanceType, "InstanceType is required" );
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( availabilityZone,
        "AvailabilityZone is required" );

    persist( new ReportingInstanceCreateEvent(
        uuid,
        instanceId,
        timestampMs,
        instanceType,
        userId,
        availabilityZone ) );
  }

  public void insertUsageEvent( @Nonnull final String uuid,
                                @Nonnull final Long timestamp,
                                @Nonnull final String metric,
                                @Nonnull final Long sequenceNum,
                                @Nonnull final String dimension,
                                @Nonnull final Double value ) {

    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( timestamp, "Timestamp is required" );
    Preconditions.checkNotNull( metric, "Metric is required" );
    Preconditions.checkNotNull( sequenceNum, "SequenceNum is required" );
    Preconditions.checkNotNull( dimension, "Dimension is required" );
    Preconditions.checkNotNull( value, "value is required" );

    persist( new ReportingInstanceUsageEvent( uuid, metric, sequenceNum, dimension, value, timestamp ) );
  }
}
