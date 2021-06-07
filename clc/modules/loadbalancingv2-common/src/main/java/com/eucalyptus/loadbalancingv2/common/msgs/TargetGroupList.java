/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetGroupList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "member")
  private ArrayList<TargetGroupTuple> member = new ArrayList<>();

  public ArrayList<TargetGroupTuple> getMember() {
    return member;
  }

  public void setMember(final ArrayList<TargetGroupTuple> member) {
    this.member = member;
  }
}
