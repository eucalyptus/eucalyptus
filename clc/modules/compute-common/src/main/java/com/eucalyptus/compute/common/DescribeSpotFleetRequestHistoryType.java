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
package com.eucalyptus.compute.common;

public class DescribeSpotFleetRequestHistoryType extends SpotInstanceMessage {

  private String eventType;
  private Integer maxResults;
  private String nextToken;
  private String spotFleetRequestId;
  private String startTime;

  public String getEventType( ) {
    return eventType;
  }

  public void setEventType( String eventType ) {
    this.eventType = eventType;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }

  public String getSpotFleetRequestId( ) {
    return spotFleetRequestId;
  }

  public void setSpotFleetRequestId( String spotFleetRequestId ) {
    this.spotFleetRequestId = spotFleetRequestId;
  }

  public String getStartTime( ) {
    return startTime;
  }

  public void setStartTime( String startTime ) {
    this.startTime = startTime;
  }
}
