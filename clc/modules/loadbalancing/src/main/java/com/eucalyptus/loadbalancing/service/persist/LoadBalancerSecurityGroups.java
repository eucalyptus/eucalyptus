/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;

import java.util.List;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroup;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public interface LoadBalancerSecurityGroups {

  <T> List<T> listByExample(LoadBalancerSecurityGroup example,
      Predicate<? super LoadBalancerSecurityGroup> filter,
      Function<? super LoadBalancerSecurityGroup, T> transform)
      throws LoadBalancingMetadataException;
}
