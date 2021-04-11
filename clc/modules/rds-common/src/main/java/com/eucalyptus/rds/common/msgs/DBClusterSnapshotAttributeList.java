/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterSnapshotAttributeList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "DBClusterSnapshotAttribute")
  private ArrayList<DBClusterSnapshotAttribute> member = new ArrayList<>();

  public ArrayList<DBClusterSnapshotAttribute> getMember() {
    return member;
  }

  public void setMember(final ArrayList<DBClusterSnapshotAttribute> member) {
    this.member = member;
  }
}
