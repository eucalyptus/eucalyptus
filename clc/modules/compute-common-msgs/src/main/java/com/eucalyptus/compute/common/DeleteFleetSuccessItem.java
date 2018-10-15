/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteFleetSuccessItem extends EucalyptusData {

  private String currentFleetState;
  private String fleetId;
  private String previousFleetState;

  public String getCurrentFleetState( ) {
    return currentFleetState;
  }

  public void setCurrentFleetState( final String currentFleetState ) {
    this.currentFleetState = currentFleetState;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public String getPreviousFleetState( ) {
    return previousFleetState;
  }

  public void setPreviousFleetState( final String previousFleetState ) {
    this.previousFleetState = previousFleetState;
  }

}
