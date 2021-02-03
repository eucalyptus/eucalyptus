/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;


@Immutable
public interface LoadBalancerPolicyAttributeTypeDescriptionView {

  String getAttributeName( );

  String getAttributeType( );

  String getCardinality( );

  @Nullable
  String getDefaultValue( );

  @Nullable
  String getDescription( );

}
