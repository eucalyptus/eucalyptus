/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class InstanceIpv6AddressListRequest extends EucalyptusData {

  private ArrayList<InstanceIpv6AddressRequest> member = new ArrayList<InstanceIpv6AddressRequest>();

  public ArrayList<InstanceIpv6AddressRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<InstanceIpv6AddressRequest> member ) {
    this.member = member;
  }
}
