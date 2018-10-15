/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribePrincipalIdFormatResponseType extends ComputeMessage {


  private String nextToken;
  private PrincipalIdFormatList principals;

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public PrincipalIdFormatList getPrincipals( ) {
    return principals;
  }

  public void setPrincipals( final PrincipalIdFormatList principals ) {
    this.principals = principals;
  }

}
