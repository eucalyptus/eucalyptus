/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RulePriorityPair extends EucalyptusData {

  @FieldRange(min = 1, max = 50000)
  private Integer priority;

  private String ruleArn;

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(final Integer priority) {
    this.priority = priority;
  }

  public String getRuleArn() {
    return ruleArn;
  }

  public void setRuleArn(final String ruleArn) {
    this.ruleArn = ruleArn;
  }

}
