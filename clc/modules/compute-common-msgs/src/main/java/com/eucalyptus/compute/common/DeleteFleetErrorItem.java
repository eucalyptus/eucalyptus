/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteFleetErrorItem extends EucalyptusData {

  private DeleteFleetError error;
  private String fleetId;

  public DeleteFleetError getError( ) {
    return error;
  }

  public void setError( final DeleteFleetError error ) {
    this.error = error;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

}
