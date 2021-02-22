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


public class SourceRegionList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "SourceRegion")
  private ArrayList<SourceRegion> member = new ArrayList<>();

  public ArrayList<SourceRegion> getMember() {
    return member;
  }

  public void setMember(final ArrayList<SourceRegion> member) {
    this.member = member;
  }
}
