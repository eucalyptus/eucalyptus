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

