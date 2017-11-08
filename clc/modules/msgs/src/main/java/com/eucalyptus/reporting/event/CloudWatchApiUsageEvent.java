/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.event;

import javax.annotation.Nonnull;
import com.eucalyptus.event.Event;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class CloudWatchApiUsageEvent implements Event {
  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String host;
  @Nonnull
  private final String accountId;
  @Nonnull
  private final String operation;
  private final int requestCount;
  private final long startTime;
  private final long endTime;

  private CloudWatchApiUsageEvent(
      @Nonnull final String host,
      @Nonnull final String accountId,
      @Nonnull final String operation,
               final int requestCount,
               final long startTime,
               final long endTime
  ) {
    this.host = Assert.notNull( host, "host" );
    this.accountId = Assert.notNull( accountId, "accountId" );
    this.operation = Assert.notNull( operation, "operation" );
    this.requestCount = requestCount;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public static CloudWatchApiUsageEvent of(
      @Nonnull final String host,
      @Nonnull final String accountId,
      @Nonnull final String operation,
      final int requestCount,
      final long startTime,
      final long endTime
  ) {
    return new CloudWatchApiUsageEvent( host, accountId, operation, requestCount, startTime, endTime );
  }

  @Nonnull
  public String getHost( ) {
    return host;
  }

  @Nonnull
  public String getAccountId( ) {
    return accountId;
  }

  @Nonnull
  public String getOperation( ) {
    return operation;
  }

  public int getRequestCount( ) {
    return requestCount;
  }

  public long getStartTime( ) {
    return startTime;
  }

  public long getEndTime( ) {
    return endTime;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper( CloudWatchApiUsageEvent.class )
        .add( "host", getHost( ) )
        .add( "accountId", getAccountId( ) )
        .add( "operation", getOperation( ) )
        .add( "requestCount", getRequestCount( ) )
        .add( "startTime", getStartTime( ) )
        .add( "endTime", getEndTime( ) )
        .toString( );
  }
}
