/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class UnsuccessfulItemSet extends EucalyptusData {

  private ArrayList<UnsuccessfulItem> member = new ArrayList<UnsuccessfulItem>();

  public ArrayList<UnsuccessfulItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<UnsuccessfulItem> member ) {
    this.member = member;
  }
}
