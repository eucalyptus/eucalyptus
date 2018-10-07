/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
