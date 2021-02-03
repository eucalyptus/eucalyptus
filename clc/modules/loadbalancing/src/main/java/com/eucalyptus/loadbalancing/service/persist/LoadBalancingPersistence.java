/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;

import org.immutables.value.Value.Immutable;


@Immutable
public interface LoadBalancingPersistence {
  LoadBalancers balancers();
  LoadBalancerSecurityGroups balancerSecurityGroups();
}
