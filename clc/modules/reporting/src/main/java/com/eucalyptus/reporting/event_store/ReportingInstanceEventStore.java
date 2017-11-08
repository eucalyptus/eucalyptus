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

import java.util.*;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

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

    persist( new ReportingInstanceUsageEvent( uuid, metric, sequenceNum,
    		dimension, value, timestamp ) );
  }

  

}
