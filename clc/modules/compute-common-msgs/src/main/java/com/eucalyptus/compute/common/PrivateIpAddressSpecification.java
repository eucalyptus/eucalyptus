/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
