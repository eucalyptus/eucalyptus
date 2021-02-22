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


public class DBClusterParameterGroupList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "DBClusterParameterGroup")
  private ArrayList<DBClusterParameterGroup> member = new ArrayList<>();

  public ArrayList<DBClusterParameterGroup> getMember() {
    return member;
  }

  public void setMember(final ArrayList<DBClusterParameterGroup> member) {
    this.member = member;
  }
}
