/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class FleetSet extends EucalyptusData {

  private ArrayList<FleetData> member = new ArrayList<FleetData>();

  public ArrayList<FleetData> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<FleetData> member ) {
    this.member = member;
  }
}
