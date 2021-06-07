/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateDefaultSubnetResponseType extends VpcMessage {

  private SubnetType subnet;

  public SubnetType getSubnet( ) {
    return subnet;
  }

  public void setSubnet( final SubnetType subnet ) {
    this.subnet = subnet;
  }
}
