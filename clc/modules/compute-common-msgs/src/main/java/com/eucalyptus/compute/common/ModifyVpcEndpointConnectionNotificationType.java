/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
