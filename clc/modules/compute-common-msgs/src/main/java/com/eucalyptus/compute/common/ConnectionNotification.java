/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ConnectionNotification extends EucalyptusData {

  private ValueStringList connectionEvents;
  private String connectionNotificationArn;
  private String connectionNotificationId;
  private String connectionNotificationState;
  private String connectionNotificationType;
  private String serviceId;
  private String vpcEndpointId;

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

  public String getConnectionNotificationState( ) {
    return connectionNotificationState;
  }

  public void setConnectionNotificationState( final String connectionNotificationState ) {
    this.connectionNotificationState = connectionNotificationState;
  }

  public String getConnectionNotificationType( ) {
    return connectionNotificationType;
  }

  public void setConnectionNotificationType( final String connectionNotificationType ) {
    this.connectionNotificationType = connectionNotificationType;
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
