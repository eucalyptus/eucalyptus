/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.loadbalancingv2.common.policy.Loadbalancingv2PolicySpec;

@PolicyVendor(Loadbalancingv2PolicySpec.VENDOR_LOADBALANCINGV2)
public interface Loadbalancingv2Metadata extends RestrictedType {

  //TODO add policy resource types
  //@PolicyResourceType( "lower_case_name-here" )
  //interface XXXMetadata extends Loadbalancingv2Metadata {}

}
