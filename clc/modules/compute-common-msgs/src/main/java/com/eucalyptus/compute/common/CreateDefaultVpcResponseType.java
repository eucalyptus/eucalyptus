/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateDefaultVpcResponseType extends VpcMessage {


  private VpcType vpc;

  public VpcType getVpc( ) {
    return vpc;
  }

  public void setVpc( final VpcType vpc ) {
    this.vpc = vpc;
  }
}
