/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;


@ComponentNamed("persistenceLoadBalancersV2")
public class PersistenceLoadBalancers extends Loadbalancingv2PersistenceSupport<Loadbalancingv2Metadata.LoadbalancerMetadata, LoadBalancer>
    implements LoadBalancers {

  public PersistenceLoadBalancers(){
    super("loadbalancer");
  }

  @Override
  protected LoadBalancer exampleWithOwner(final OwnerFullName ownerFullName){
    return LoadBalancer.named(ownerFullName,null);
  }

  @Override
  protected LoadBalancer exampleWithName(final OwnerFullName ownerFullName,final String name){
    return LoadBalancer.named(ownerFullName,name);
  }
}