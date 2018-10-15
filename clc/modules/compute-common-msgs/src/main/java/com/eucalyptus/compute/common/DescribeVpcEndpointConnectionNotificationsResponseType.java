/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeVpcEndpointConnectionNotificationsResponseType extends VpcMessage {


  private ConnectionNotificationSet connectionNotificationSet;
  private String nextToken;

  public ConnectionNotificationSet getConnectionNotificationSet( ) {
    return connectionNotificationSet;
  }

  public void setConnectionNotificationSet( final ConnectionNotificationSet connectionNotificationSet ) {
    this.connectionNotificationSet = connectionNotificationSet;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
