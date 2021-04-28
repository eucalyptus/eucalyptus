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
import com.eucalyptus.util.techpreview.TechPreview;

/**
 *
 */
@PublicService
@AwsServiceName("elasticloadbalancing")
@PolicyVendor("elasticloadbalancing")
@Partition(value = Loadbalancingv2.class, manyToOne = true)
@Description("ELB v2 API service")
@TechPreview(enableByDefaultProperty = "enable.loadbalancingv2.tech.preview")
public class Loadbalancingv2 extends ComponentId {

  private static final long serialVersionUID = 1L;
}
