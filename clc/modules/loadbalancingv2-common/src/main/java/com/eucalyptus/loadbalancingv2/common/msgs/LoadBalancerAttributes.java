/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadBalancerAttributes extends EucalyptusData {

  @FieldRange(max = 20)
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "member")
  private ArrayList<LoadBalancerAttribute> member = new ArrayList<>();

  public ArrayList<LoadBalancerAttribute> getMember() {
    return member;
  }

  public void setMember(final ArrayList<LoadBalancerAttribute> member) {
    this.member = member;
  }
}
