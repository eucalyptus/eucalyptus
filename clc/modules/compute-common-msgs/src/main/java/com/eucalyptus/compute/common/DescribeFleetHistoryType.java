/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class DescribeFleetHistoryType extends ComputeMessage {

  private String eventType;
  @Nonnull
  private String fleetId;
  private Integer maxResults;
  private String nextToken;
  @Nonnull
  private java.util.Date startTime;

  public String getEventType( ) {
    return eventType;
  }

  public void setEventType( final String eventType ) {
    this.eventType = eventType;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( final Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public java.util.Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( final java.util.Date startTime ) {
    this.startTime = startTime;
  }

}
