/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SecurityGroups extends EucalyptusData {

  @FieldRegex(FieldRegexValue.EC2_SECURITYGROUP)
  @HttpParameterMapping(parameter = "member")
  private ArrayList<String> member = new ArrayList<>();

  public ArrayList<String> getMember() {
    return member;
  }

  public void setMember(final ArrayList<String> member) {
    this.member = member;
  }
}
