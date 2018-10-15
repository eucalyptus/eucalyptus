/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeVpcEndpointConnectionsResponseType extends VpcMessage {


  private String nextToken;
  private VpcEndpointConnectionSet vpcEndpointConnections;

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public VpcEndpointConnectionSet getVpcEndpointConnections( ) {
    return vpcEndpointConnections;
  }

  public void setVpcEndpointConnections( final VpcEndpointConnectionSet vpcEndpointConnections ) {
    this.vpcEndpointConnections = vpcEndpointConnections;
  }

}
