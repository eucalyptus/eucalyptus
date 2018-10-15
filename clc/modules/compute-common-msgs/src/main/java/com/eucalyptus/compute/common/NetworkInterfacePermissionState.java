/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class NetworkInterfacePermissionState extends EucalyptusData {

  private String state;
  private String statusMessage;

  public String getState( ) {
    return state;
  }

  public void setState( final String state ) {
    this.state = state;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( final String statusMessage ) {
    this.statusMessage = statusMessage;
  }

}
