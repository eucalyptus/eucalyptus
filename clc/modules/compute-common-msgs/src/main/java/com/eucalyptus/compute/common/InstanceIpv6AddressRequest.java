/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class InstanceIpv6AddressRequest extends EucalyptusData {

  private String ipv6Address;

  public String getIpv6Address( ) {
    return ipv6Address;
  }

  public void setIpv6Address( final String ipv6Address ) {
    this.ipv6Address = ipv6Address;
  }

}
