/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.policy;

import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.policy.Loadbalancingv2PolicySpec;


@PolicyKey(Loadbalancingv2TargetGroupNumberQuotaKey.KEY)
public class Loadbalancingv2TargetGroupNumberQuotaKey extends Loadbalancingv2NumberQuotaKeySupport<Loadbalancingv2Metadata.TargetgroupMetadata> {

  public static final String KEY = "elb:quota-targetgroupnumber";

  public Loadbalancingv2TargetGroupNumberQuotaKey() {
    super(
        KEY,
        Loadbalancingv2PolicySpec.LOADBALANCINGV2_CREATETARGETGROUP,
        Loadbalancingv2Metadata.TargetgroupMetadata.class);
  }
}
