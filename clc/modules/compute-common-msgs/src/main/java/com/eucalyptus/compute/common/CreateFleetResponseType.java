/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateFleetResponseType extends ComputeMessage {

  private String fleetId;

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

}
