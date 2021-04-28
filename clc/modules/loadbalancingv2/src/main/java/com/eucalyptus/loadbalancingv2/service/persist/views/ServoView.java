/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import org.immutables.value.Value;

@Value.Immutable
public interface ServoView {

  String getDisplayName();

  String getLoadBalancerId();

  String getLoadBalancerName();

  String getOwnerAccountNumber();

  String getAvailabiltyZone();

  String getIpAddress();
}
