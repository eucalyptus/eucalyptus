/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateVpcEndpointConnectionNotificationResponseType extends VpcMessage {


  private String clientToken;
  private ConnectionNotification connectionNotification;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public ConnectionNotification getConnectionNotification( ) {
    return connectionNotification;
  }

  public void setConnectionNotification( final ConnectionNotification connectionNotification ) {
    this.connectionNotification = connectionNotification;
  }

}
