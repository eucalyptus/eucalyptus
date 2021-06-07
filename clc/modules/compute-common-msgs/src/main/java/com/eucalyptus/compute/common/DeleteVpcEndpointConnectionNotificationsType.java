/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpParameterMapping;


public class DeleteVpcEndpointConnectionNotificationsType extends VpcMessage {

  @HttpParameterMapping( parameter = "ConnectionNotificationId" )
  @Nonnull
  private ValueStringList connectionNotificationIds;

  public ValueStringList getConnectionNotificationIds( ) {
    return connectionNotificationIds;
  }

  public void setConnectionNotificationIds( final ValueStringList connectionNotificationIds ) {
    this.connectionNotificationIds = connectionNotificationIds;
  }

}
