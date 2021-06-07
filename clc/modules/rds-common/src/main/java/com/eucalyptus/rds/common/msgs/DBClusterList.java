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


public class DBClusterList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "DBCluster")
  private ArrayList<DBCluster> member = new ArrayList<>();

  public ArrayList<DBCluster> getMember() {
    return member;
  }

  public void setMember(final ArrayList<DBCluster> member) {
    this.member = member;
  }
}
