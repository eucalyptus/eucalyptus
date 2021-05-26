/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.loadbalancingv2.common.policy.Loadbalancingv2PolicySpec;

@PolicyVendor(Loadbalancingv2PolicySpec.VENDOR_LOADBALANCINGV2)
public interface Loadbalancingv2Metadata extends RestrictedType {

  @PolicyResourceType( "listener" )
  interface ListenerMetadata extends Loadbalancingv2Metadata {}

  @PolicyResourceType( "listener-rule" )
  interface ListenerRuleMetadata extends Loadbalancingv2Metadata {}

  @PolicyResourceType( "loadbalancer" )
  interface LoadbalancerMetadata extends Loadbalancingv2Metadata {}

  @PolicyResourceType( "targetgroup" )
  interface TargetgroupMetadata extends Loadbalancingv2Metadata {}

  @PolicyResourceType( "tag" )
  interface TagMetadata extends Loadbalancingv2Metadata {}

}
