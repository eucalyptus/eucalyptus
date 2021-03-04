/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class DeleteRuleType extends Loadbalancingv2Message {

  @Nonnull
  private String ruleArn;

  public String getRuleArn() {
    return ruleArn;
  }

  public void setRuleArn(final String ruleArn) {
    this.ruleArn = ruleArn;
  }

}
