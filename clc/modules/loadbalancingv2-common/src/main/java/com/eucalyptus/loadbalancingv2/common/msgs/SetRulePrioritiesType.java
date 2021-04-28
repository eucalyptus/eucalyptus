/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class SetRulePrioritiesType extends Loadbalancingv2Message {

  @Nonnull
  private RulePriorityList rulePriorities;

  public RulePriorityList getRulePriorities() {
    return rulePriorities;
  }

  public void setRulePriorities(final RulePriorityList rulePriorities) {
    this.rulePriorities = rulePriorities;
  }

}
