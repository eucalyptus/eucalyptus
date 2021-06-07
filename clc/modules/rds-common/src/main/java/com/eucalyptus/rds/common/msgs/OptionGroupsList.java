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


public class OptionGroupsList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "OptionGroup")
  private ArrayList<OptionGroup> member = new ArrayList<>();

  public ArrayList<OptionGroup> getMember() {
    return member;
  }

  public void setMember(final ArrayList<OptionGroup> member) {
    this.member = member;
  }
}
