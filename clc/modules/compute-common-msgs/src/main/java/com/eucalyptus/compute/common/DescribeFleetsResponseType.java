/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
