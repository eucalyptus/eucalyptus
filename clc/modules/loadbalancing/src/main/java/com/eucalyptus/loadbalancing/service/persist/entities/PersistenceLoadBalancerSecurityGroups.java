/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerSecurityGroupMetadata;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancerSecurityGroups;

@ComponentNamed
public class PersistenceLoadBalancerSecurityGroups
    extends
    LoadBalancingPersistenceSupport<LoadBalancerSecurityGroupMetadata, LoadBalancerSecurityGroup>
    implements LoadBalancerSecurityGroups {

  public PersistenceLoadBalancerSecurityGroups() {
    super("loadbalancer/securitygroup");
  }

  @Override
  protected LoadBalancerSecurityGroup exampleWithOwner(final OwnerFullName ownerFullName) {
    return LoadBalancerSecurityGroup.named(ownerFullName, null);
  }

  @Override
  protected LoadBalancerSecurityGroup exampleWithName(final OwnerFullName ownerFullName,
      final String name) {
    return LoadBalancerSecurityGroup.named(ownerFullName, name);
  }
}

