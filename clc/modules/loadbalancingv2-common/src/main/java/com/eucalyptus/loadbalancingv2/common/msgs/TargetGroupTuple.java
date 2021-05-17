/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetGroupTuple extends EucalyptusData {

  @FieldRegex(FieldRegexValue.LOADBALANCING_ARN)
  private String targetGroupArn;

  @FieldRange(max = 999)
  private Integer weight;

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(final Integer weight) {
    this.weight = weight;
  }

}
