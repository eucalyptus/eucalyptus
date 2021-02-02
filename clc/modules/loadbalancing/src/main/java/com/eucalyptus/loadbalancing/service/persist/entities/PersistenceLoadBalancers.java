/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancers;


@ComponentNamed
public class PersistenceLoadBalancers extends LoadBalancingPersistenceSupport<LoadBalancerMetadata, LoadBalancer> implements LoadBalancers {
  
  public PersistenceLoadBalancers() {
    super("loadbalancer");
  }

  @Override
  protected LoadBalancer exampleWithOwner(final OwnerFullName ownerFullName) {
    return LoadBalancer.named(ownerFullName, null);
  }

  @Override
  protected LoadBalancer exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return LoadBalancer.named(ownerFullName, name);
  }
}
