/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteFleetErrorSet extends EucalyptusData {

  private ArrayList<DeleteFleetErrorItem> member = new ArrayList<DeleteFleetErrorItem>();

  public ArrayList<DeleteFleetErrorItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<DeleteFleetErrorItem> member ) {
    this.member = member;
  }
}
