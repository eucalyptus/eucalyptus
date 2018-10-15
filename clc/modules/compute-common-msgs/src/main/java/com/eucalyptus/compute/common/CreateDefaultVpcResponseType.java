/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
