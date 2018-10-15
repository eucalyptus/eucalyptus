/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ActiveInstanceSet extends EucalyptusData {

  private ArrayList<ActiveInstance> member = new ArrayList<ActiveInstance>();

  public ArrayList<ActiveInstance> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ActiveInstance> member ) {
    this.member = member;
  }
}
