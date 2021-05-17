/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import javax.annotation.Nonnull;


public class CreateRuleType extends Loadbalancingv2Message {

  @Nonnull
  private Actions actions;

  @Nonnull
  private RuleConditionList conditions;

  @Nonnull
  @FieldRegex(FieldRegexValue.LOADBALANCING_ARN)
  private String listenerArn;

  @Nonnull
  @FieldRange(min = 1, max = 50000)
  private Integer priority;

  public Actions getActions() {
    return actions;
  }

  public void setActions(final Actions actions) {
    this.actions = actions;
  }

  public RuleConditionList getConditions() {
    return conditions;
  }

  public void setConditions(final RuleConditionList conditions) {
    this.conditions = conditions;
  }

  public String getListenerArn() {
    return listenerArn;
  }

  public void setListenerArn(final String listenerArn) {
    this.listenerArn = listenerArn;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(final Integer priority) {
    this.priority = priority;
  }

}
