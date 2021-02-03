/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.List;
import org.immutables.value.Value.Immutable;
import io.vavr.collection.Stream;


@Immutable
public interface LoadBalancerPolicyDescriptionFullView {

  LoadBalancerPolicyDescriptionView getPolicyDescription();

  List<LoadBalancerPolicyAttributeDescriptionView> getPolicyAttributeDescriptions();

  default List<LoadBalancerPolicyAttributeDescriptionView> findAttributeDescription( final String name ) {
    return Stream.ofAll( getPolicyAttributeDescriptions( ) )
        .filter( policyAttributeDescription -> name.equals(policyAttributeDescription.getAttributeName( )) )
        .toJavaList();
  }

}
