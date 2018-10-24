/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class PrivateIpAddressSpecification extends EucalyptusData {

  private Boolean primary;
  private String privateIpAddress;

  public Boolean getPrimary( ) {
    return primary;
  }

  public void setPrimary( final Boolean primary ) {
    this.primary = primary;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( final String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

}
