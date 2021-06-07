/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.policy;

import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.policy.Loadbalancingv2PolicySpec;


@PolicyKey(Loadbalancingv2LoadbalancerNumberQuotaKey.KEY)
public class Loadbalancingv2LoadbalancerNumberQuotaKey extends Loadbalancingv2NumberQuotaKeySupport<Loadbalancingv2Metadata.LoadbalancerMetadata> {

  public static final String KEY = "elb:quota-loadbalancerv2number";

  public Loadbalancingv2LoadbalancerNumberQuotaKey() {
    super(
        KEY,
        Loadbalancingv2PolicySpec.LOADBALANCINGV2_CREATELOADBALANCER,
        Loadbalancingv2Metadata.LoadbalancerMetadata.class);
  }
}
