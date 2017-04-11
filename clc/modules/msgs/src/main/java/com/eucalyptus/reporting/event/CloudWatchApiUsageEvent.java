/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
