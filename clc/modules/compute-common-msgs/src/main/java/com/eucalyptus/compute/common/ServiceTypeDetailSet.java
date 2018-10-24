/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ServiceTypeDetailSet extends EucalyptusData {

  private ArrayList<ServiceTypeDetail> member = new ArrayList<ServiceTypeDetail>();

  public ArrayList<ServiceTypeDetail> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ServiceTypeDetail> member ) {
    this.member = member;
  }
}
