/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeRulesType extends Loadbalancingv2Message {

  private String listenerArn;

  private String marker;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  private RuleArns ruleArns;

  public String getListenerArn() {
    return listenerArn;
  }

  public void setListenerArn(final String listenerArn) {
    this.listenerArn = listenerArn;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

  public RuleArns getRuleArns() {
    return ruleArns;
  }

  public void setRuleArns(final RuleArns ruleArns) {
    this.ruleArns = ruleArns;
  }

}
