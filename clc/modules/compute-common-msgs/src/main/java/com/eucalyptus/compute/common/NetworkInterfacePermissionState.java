/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
