/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
