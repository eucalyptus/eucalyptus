/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class RejectVpcEndpointConnectionsResponseType extends VpcMessage {


  private UnsuccessfulItemSet unsuccessful;

  public UnsuccessfulItemSet getUnsuccessful( ) {
    return unsuccessful;
  }

  public void setUnsuccessful( final UnsuccessfulItemSet unsuccessful ) {
    this.unsuccessful = unsuccessful;
  }

}
