/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeFleetHistoryResponseType extends ComputeMessage {


  private String fleetId;
  private HistoryRecordSet historyRecords;
  private java.util.Date lastEvaluatedTime;
  private String nextToken;
  private java.util.Date startTime;

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public HistoryRecordSet getHistoryRecords( ) {
    return historyRecords;
  }

  public void setHistoryRecords( final HistoryRecordSet historyRecords ) {
    this.historyRecords = historyRecords;
  }

  public java.util.Date getLastEvaluatedTime( ) {
    return lastEvaluatedTime;
  }

  public void setLastEvaluatedTime( final java.util.Date lastEvaluatedTime ) {
    this.lastEvaluatedTime = lastEvaluatedTime;
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
