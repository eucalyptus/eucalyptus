/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroup.STATE;


@Immutable
public interface LoadBalancerSecurityGroupView {

  String getName( );

  String getGroupOwnerAccountId( );

  STATE getState( );

}
