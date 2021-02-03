/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;


@Immutable
public interface LoadBalancerFullView {

  LoadBalancerView getLoadBalancer();

  @Nullable
  LoadBalancerSecurityGroupView getSecurityGroup();

  List<LoadBalancerAutoScalingGroupView>  getAutoScalingGroups();

  List<LoadBalancerBackendInstanceView> getBackendInstances();

  List<LoadBalancerBackendServerDescriptionFullView> getBackendServers();

  List<LoadBalancerListenerFullView> getListeners();

  List<LoadBalancerPolicyDescriptionFullView> getPolicies();

  List<LoadBalancerSecurityGroupRefView> getSecurityGroupRefs();

  List<LoadBalancerZoneView> getZones();

}
