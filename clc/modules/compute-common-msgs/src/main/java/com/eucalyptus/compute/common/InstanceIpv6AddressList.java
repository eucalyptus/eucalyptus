/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import com.eucalyptus.compute.common.InstanceIpv6Address;


public class InstanceIpv6AddressList extends EucalyptusData {

  private ArrayList<InstanceIpv6Address> member = new ArrayList<InstanceIpv6Address>();

  public ArrayList<InstanceIpv6Address> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<InstanceIpv6Address> member ) {
    this.member = member;
  }
}
