/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Rule extends EucalyptusData {

  private Actions actions;

  private RuleConditionList conditions;

  private Boolean isDefault;

  private String priority;

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

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(final Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(final String priority) {
    this.priority = priority;
  }

  public String getRuleArn() {
    return ruleArn;
  }

  public void setRuleArn(final String ruleArn) {
    this.ruleArn = ruleArn;
  }

}
