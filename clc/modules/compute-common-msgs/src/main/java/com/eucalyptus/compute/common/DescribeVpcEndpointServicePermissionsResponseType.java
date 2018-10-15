/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeVpcEndpointServicePermissionsResponseType extends VpcMessage {


  private AllowedPrincipalSet allowedPrincipals;
  private String nextToken;

  public AllowedPrincipalSet getAllowedPrincipals( ) {
    return allowedPrincipals;
  }

  public void setAllowedPrincipals( final AllowedPrincipalSet allowedPrincipals ) {
    this.allowedPrincipals = allowedPrincipals;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
