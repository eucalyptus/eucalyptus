/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyRuleResult extends EucalyptusData {

  private Rules rules;

  public Rules getRules() {
    return rules;
  }

  public void setRules(final Rules rules) {
    this.rules = rules;
  }

}
