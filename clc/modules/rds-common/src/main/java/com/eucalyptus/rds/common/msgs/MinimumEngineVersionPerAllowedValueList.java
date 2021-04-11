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


public class MinimumEngineVersionPerAllowedValueList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "MinimumEngineVersionPerAllowedValue")
  private ArrayList<MinimumEngineVersionPerAllowedValue> member = new ArrayList<>();

  public ArrayList<MinimumEngineVersionPerAllowedValue> getMember() {
    return member;
  }

  public void setMember(final ArrayList<MinimumEngineVersionPerAllowedValue> member) {
    this.member = member;
  }
}
