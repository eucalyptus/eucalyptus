/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyVpcEndpointConnectionNotificationType extends ComputeMessage {

  private ValueStringList connectionEvents;
  private String connectionNotificationArn;
  @Nonnull
  private String connectionNotificationId;

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

  public String getConnectionNotificationId( ) {
    return connectionNotificationId;
  }

  public void setConnectionNotificationId( final String connectionNotificationId ) {
    this.connectionNotificationId = connectionNotificationId;
  }

}
