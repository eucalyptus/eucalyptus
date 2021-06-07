/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class HistoryRecordSet extends EucalyptusData {

  private ArrayList<HistoryRecordEntry> member = new ArrayList<HistoryRecordEntry>();

  public ArrayList<HistoryRecordEntry> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<HistoryRecordEntry> member ) {
    this.member = member;
  }
}
