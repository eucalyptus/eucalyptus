/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PublicService;

/**
 *
 */
@PublicService
@AwsServiceName("elasticloadbalancing")
@PolicyVendor("elasticloadbalancing")
@Partition(value = Loadbalancingv2.class, manyToOne = true)
@Description("ELB v2 API service")
public class Loadbalancingv2 extends ComponentId {

  private static final long serialVersionUID = 1L;
}
