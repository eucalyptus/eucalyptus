/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class VolumeModificationList extends EucalyptusData {

  private ArrayList<VolumeModification> member = new ArrayList<VolumeModification>();

  public ArrayList<VolumeModification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<VolumeModification> member ) {
    this.member = member;
  }
}
