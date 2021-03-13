/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import com.eucalyptus.loadbalancingv2.common.msgs.Actions;
import com.eucalyptus.loadbalancingv2.common.msgs.RuleConditionList;
import com.eucalyptus.loadbalancingv2.service.persist.JsonEncoding;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.util.Json;
import org.immutables.value.Value;


@Value.Immutable
public interface ListenerRuleView {

  String getNaturalId();

  String getOwnerAccountNumber();

  LoadBalancer.Type getLoadBalancerType();

  String getLoadBalancerId();

  String getLoadBalancerName();

  String getListenerId();

  default String getArn() {
    return String.format(
        "arn:aws:elasticloadbalancing::%1s:listener-rule/%2s/%3s/%4s/%5s/%6s",
        getOwnerAccountNumber(),
        getLoadBalancerType().getCode(),
        getLoadBalancerName(),
        getLoadBalancerId(),
        getListenerId(),
        getNaturalId());
  }

  Integer getPriority();

  String getActions();

  String getConditions();

  default Actions getRuleActions() {
    return JsonEncoding.read(Actions.class, getActions()).getOrElse(Actions::new);
  }

  default RuleConditionList getRuleConditions() {
    return JsonEncoding.read(RuleConditionList.class, getConditions()).getOrElse(RuleConditionList::new);
  }
}
