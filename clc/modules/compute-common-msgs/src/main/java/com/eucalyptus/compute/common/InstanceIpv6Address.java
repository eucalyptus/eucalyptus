/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class InstanceIpv6Address extends EucalyptusData {

  private String ipv6Address;

  public String getIpv6Address( ) {
    return ipv6Address;
  }

  public void setIpv6Address( final String ipv6Address ) {
    this.ipv6Address = ipv6Address;
  }

}
