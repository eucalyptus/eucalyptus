/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerZone.STATE;


@Immutable
public interface LoadBalancerZoneView {

  String getName( );

  @Nullable
  String getSubnetId( );

  STATE getState( );

  @Nullable
  String getAutoscalingGroup( );

}
