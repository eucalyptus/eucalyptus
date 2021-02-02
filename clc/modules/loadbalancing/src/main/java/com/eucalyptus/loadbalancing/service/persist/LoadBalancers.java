/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;

import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.google.common.base.Function;


public interface LoadBalancers {

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super LoadBalancer,T> transform ) throws LoadBalancingMetadataException;

  LoadBalancer save(LoadBalancer loadBalancer) throws LoadBalancingMetadataException;

  @QuantityMetricFunction( LoadBalancerMetadata.class )
  enum CountLoadBalancers implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
        return Entities.count( LoadBalancer.named( input, null ) );
      }
    }
  }

}
