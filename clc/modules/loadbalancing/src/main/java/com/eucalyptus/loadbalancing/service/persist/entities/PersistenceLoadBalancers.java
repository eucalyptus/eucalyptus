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
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;


@ComponentNamed
public class PersistenceLoadBalancers extends LoadBalancingPersistenceSupport<LoadBalancerMetadata, LoadBalancer> implements LoadBalancers {
  
  public PersistenceLoadBalancers() {
    super("loadbalancer");
  }

  @Override
  public <T> T updateByExample(
      final LoadBalancer example,
      final OwnerFullName ownerFullName,
      final String key,
      final Predicate<? super LoadBalancer> filter,
      final Function<? super LoadBalancer, T> updateTransform
  ) throws LoadBalancingMetadataException {
    return updateByExample( example, ownerFullName, key, balancer -> {
      if ( !filter.apply( balancer ) ) {
        throw Exceptions.toUndeclared( notFoundException(
            qualifyOwner( "Filter denied "+typeDescription+" '"+key+"'", ownerFullName ), null ) );
      }
      return updateTransform.apply( balancer );
    } );
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
