/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;


@Immutable
public interface LoadBalancerAutoScalingGroupView {

  String getName( );

  int getCapacity( );

  String getAvailabilityZone( );

  @Nullable
  String getUserSubnetId( );

  @Nullable
  String getSystemSubnetId( );

}
