/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeFleetsResponseType extends ComputeMessage {


  private FleetSet fleets;
  private String nextToken;

  public FleetSet getFleets( ) {
    return fleets;
  }

  public void setFleets( final FleetSet fleets ) {
    this.fleets = fleets;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
