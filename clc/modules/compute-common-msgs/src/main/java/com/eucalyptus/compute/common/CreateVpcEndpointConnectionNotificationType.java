/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class CreateVpcEndpointConnectionNotificationType extends VpcMessage {

  private String clientToken;
  @Nonnull
  private ValueStringList connectionEvents;
  @Nonnull
  private String connectionNotificationArn;
  private String serviceId;
  private String vpcEndpointId;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public ValueStringList getConnectionEvents( ) {
    return connectionEvents;
  }

  public void setConnectionEvents( final ValueStringList connectionEvents ) {
    this.connectionEvents = connectionEvents;
  }

  public String getConnectionNotificationArn( ) {
    return connectionNotificationArn;
  }

  public void setConnectionNotificationArn( final String connectionNotificationArn ) {
    this.connectionNotificationArn = connectionNotificationArn;
  }

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

  public String getVpcEndpointId( ) {
    return vpcEndpointId;
  }

  public void setVpcEndpointId( final String vpcEndpointId ) {
    this.vpcEndpointId = vpcEndpointId;
  }

}
