/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import com.eucalyptus.loadbalancingv2.common.msgs.Actions;
import com.eucalyptus.loadbalancingv2.service.persist.JsonEncoding;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Listener;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import org.immutables.value.Value;


@Value.Immutable
public interface ListenerView {

  String getNaturalId();

  String getOwnerAccountNumber();

  LoadBalancer.Type getLoadBalancerType();

  String getLoadBalancerId();

  String getLoadBalancerName();

  default String getArn() {
    return String.format(
        "arn:aws:elasticloadbalancing::%1s:listener/%2s/%3s/%4s/%5s",
        getOwnerAccountNumber(),
        getLoadBalancerType().getCode(),
        getLoadBalancerName(),
        getLoadBalancerId(),
        getNaturalId());
  }

  default String getLoadbalancerArn() {
    return String.format(
        "arn:aws:elasticloadbalancing::%1s:loadbalancer/%2s/%3s/%4s",
        getOwnerAccountNumber(),
        getLoadBalancerType().getCode(),
        getLoadBalancerName(),
        getLoadBalancerId());
  }

  Integer getPort();

  Listener.Protocol getProtocol();

  String getSslPolicy();

  String getDefaultActions();

  default Actions getListenerDefaultActions() {
    return JsonEncoding.read(Actions.class, getDefaultActions()).getOrElse(Actions::new);
  }
}
