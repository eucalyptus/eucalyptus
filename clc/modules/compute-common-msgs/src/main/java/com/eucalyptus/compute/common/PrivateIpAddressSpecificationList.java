/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class PrivateIpAddressSpecificationList extends EucalyptusData {

  private ArrayList<PrivateIpAddressSpecification> member = new ArrayList<PrivateIpAddressSpecification>();

  public ArrayList<PrivateIpAddressSpecification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<PrivateIpAddressSpecification> member ) {
    this.member = member;
  }
}
