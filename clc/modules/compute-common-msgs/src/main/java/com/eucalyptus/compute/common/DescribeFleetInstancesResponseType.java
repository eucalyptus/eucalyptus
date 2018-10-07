/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeFleetInstancesResponseType extends ComputeMessage {

  private ActiveInstanceSet activeInstances;
  private String fleetId;
  private String nextToken;

  public ActiveInstanceSet getActiveInstances( ) {
    return activeInstances;
  }

  public void setActiveInstances( final ActiveInstanceSet activeInstances ) {
    this.activeInstances = activeInstances;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
