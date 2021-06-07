/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteFleetSuccessSet extends EucalyptusData {

  private ArrayList<DeleteFleetSuccessItem> member = new ArrayList<DeleteFleetSuccessItem>();

  public ArrayList<DeleteFleetSuccessItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<DeleteFleetSuccessItem> member ) {
    this.member = member;
  }
}
