/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface LoadBalancerListenersView {

  LoadBalancerView getLoadBalancer();

  List<ListenerView> getListeners();
}
