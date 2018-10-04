/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
