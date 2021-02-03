/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;

import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerPolicyTypeDescription;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyAttributeTypeDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyTypeDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyTypeDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyTypeDescriptionFullView;
import com.eucalyptus.util.CompatFunction;
import io.vavr.collection.Stream;

/**
 *
 */
public interface LoadBalancerPolicyTypeDescriptions {
  CompatFunction<LoadBalancerPolicyTypeDescription, LoadBalancerPolicyTypeDescriptionFullView> FULL_VIEW =
      policyTypeDescription -> ImmutableLoadBalancerPolicyTypeDescriptionFullView.builder()
          .policyTypeDescription( ImmutableLoadBalancerPolicyTypeDescriptionView.copyOf( policyTypeDescription ) )
          .policyAttributeTypeDescriptions( Stream.ofAll( policyTypeDescription.getPolicyAttributeTypeDescriptions() )
              .map( ImmutableLoadBalancerPolicyAttributeTypeDescriptionView::copyOf ) )
          .build();
}
