/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AllowedPrincipal extends EucalyptusData {

  private String principal;
  private String principalType;

  public String getPrincipal( ) {
    return principal;
  }

  public void setPrincipal( final String principal ) {
    this.principal = principal;
  }

  public String getPrincipalType( ) {
    return principalType;
  }

  public void setPrincipalType( final String principalType ) {
    this.principalType = principalType;
  }

}
