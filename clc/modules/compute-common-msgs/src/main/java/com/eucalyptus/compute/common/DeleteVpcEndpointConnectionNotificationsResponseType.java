/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DeleteVpcEndpointConnectionNotificationsResponseType extends VpcMessage {


  private UnsuccessfulItemSet unsuccessful;

  public UnsuccessfulItemSet getUnsuccessful( ) {
    return unsuccessful;
  }

  public void setUnsuccessful( final UnsuccessfulItemSet unsuccessful ) {
    this.unsuccessful = unsuccessful;
  }

}
