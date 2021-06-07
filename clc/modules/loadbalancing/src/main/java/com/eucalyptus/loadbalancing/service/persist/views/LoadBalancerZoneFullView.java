/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
public interface LoadBalancerZoneFullView {

  LoadBalancerZoneView getZone();

  List<LoadBalancerBackendInstanceView> getBackendInstances();

  List<LoadBalancerServoInstanceView> getServoInstances();
}
