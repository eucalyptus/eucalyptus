/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class ModifyRuleType extends Loadbalancingv2Message {

  private Actions actions;

  private RuleConditionList conditions;

  @Nonnull
  private String ruleArn;

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

  public String getRuleArn() {
    return ruleArn;
  }

  public void setRuleArn(final String ruleArn) {
    this.ruleArn = ruleArn;
  }

}
