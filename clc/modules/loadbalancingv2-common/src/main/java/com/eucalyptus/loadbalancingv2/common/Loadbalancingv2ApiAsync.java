/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancingv2.common.msgs.AddListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeregisterTargetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeregisterTargetsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeAccountLimitsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeAccountLimitsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenersResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenersType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancerAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeRulesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeRulesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeSSLPoliciesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeSSLPoliciesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetHealthResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetHealthType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyLoadBalancerAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.RegisterTargetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RegisterTargetsType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetIpAddressTypeResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetIpAddressTypeType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetRulePrioritiesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetRulePrioritiesType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSecurityGroupsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSecurityGroupsType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSubnetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSubnetsType;
import com.eucalyptus.util.async.CheckedListenableFuture;


@ComponentPart(Loadbalancingv2.class)
public interface Loadbalancingv2ApiAsync {

  CheckedListenableFuture<AddListenerCertificatesResponseType> addListenerCertificatesAsync(final AddListenerCertificatesType request);

  CheckedListenableFuture<AddTagsResponseType> addTagsAsync(final AddTagsType request);

  CheckedListenableFuture<CreateListenerResponseType> createListenerAsync(final CreateListenerType request);

  CheckedListenableFuture<CreateLoadBalancerResponseType> createLoadBalancerAsync(final CreateLoadBalancerType request);

  CheckedListenableFuture<CreateRuleResponseType> createRuleAsync(final CreateRuleType request);

  CheckedListenableFuture<CreateTargetGroupResponseType> createTargetGroupAsync(final CreateTargetGroupType request);

  CheckedListenableFuture<DeleteListenerResponseType> deleteListenerAsync(final DeleteListenerType request);

  CheckedListenableFuture<DeleteLoadBalancerResponseType> deleteLoadBalancerAsync(final DeleteLoadBalancerType request);

  CheckedListenableFuture<DeleteRuleResponseType> deleteRuleAsync(final DeleteRuleType request);

  CheckedListenableFuture<DeleteTargetGroupResponseType> deleteTargetGroupAsync(final DeleteTargetGroupType request);

  CheckedListenableFuture<DeregisterTargetsResponseType> deregisterTargetsAsync(final DeregisterTargetsType request);

  CheckedListenableFuture<DescribeAccountLimitsResponseType> describeAccountLimitsAsync(final DescribeAccountLimitsType request);

  default CheckedListenableFuture<DescribeAccountLimitsResponseType> describeAccountLimitsAsync() {
    return describeAccountLimitsAsync(new DescribeAccountLimitsType());
  }

  CheckedListenableFuture<DescribeListenerCertificatesResponseType> describeListenerCertificatesAsync(final DescribeListenerCertificatesType request);

  CheckedListenableFuture<DescribeListenersResponseType> describeListenersAsync(final DescribeListenersType request);

  default CheckedListenableFuture<DescribeListenersResponseType> describeListenersAsync() {
    return describeListenersAsync(new DescribeListenersType());
  }

  CheckedListenableFuture<DescribeLoadBalancerAttributesResponseType> describeLoadBalancerAttributesAsync(final DescribeLoadBalancerAttributesType request);

  CheckedListenableFuture<DescribeLoadBalancersResponseType> describeLoadBalancersAsync(final DescribeLoadBalancersType request);

  default CheckedListenableFuture<DescribeLoadBalancersResponseType> describeLoadBalancersAsync() {
    return describeLoadBalancersAsync(new DescribeLoadBalancersType());
  }

  CheckedListenableFuture<DescribeRulesResponseType> describeRulesAsync(final DescribeRulesType request);

  default CheckedListenableFuture<DescribeRulesResponseType> describeRulesAsync() {
    return describeRulesAsync(new DescribeRulesType());
  }

  CheckedListenableFuture<DescribeSSLPoliciesResponseType> describeSSLPoliciesAsync(final DescribeSSLPoliciesType request);

  default CheckedListenableFuture<DescribeSSLPoliciesResponseType> describeSSLPoliciesAsync() {
    return describeSSLPoliciesAsync(new DescribeSSLPoliciesType());
  }

  CheckedListenableFuture<DescribeTagsResponseType> describeTagsAsync(final DescribeTagsType request);

  CheckedListenableFuture<DescribeTargetGroupAttributesResponseType> describeTargetGroupAttributesAsync(final DescribeTargetGroupAttributesType request);

  CheckedListenableFuture<DescribeTargetGroupsResponseType> describeTargetGroupsAsync(final DescribeTargetGroupsType request);

  default CheckedListenableFuture<DescribeTargetGroupsResponseType> describeTargetGroupsAsync() {
    return describeTargetGroupsAsync(new DescribeTargetGroupsType());
  }

  CheckedListenableFuture<DescribeTargetHealthResponseType> describeTargetHealthAsync(final DescribeTargetHealthType request);

  CheckedListenableFuture<ModifyListenerResponseType> modifyListenerAsync(final ModifyListenerType request);

  CheckedListenableFuture<ModifyLoadBalancerAttributesResponseType> modifyLoadBalancerAttributesAsync(final ModifyLoadBalancerAttributesType request);

  CheckedListenableFuture<ModifyRuleResponseType> modifyRuleAsync(final ModifyRuleType request);

  CheckedListenableFuture<ModifyTargetGroupResponseType> modifyTargetGroupAsync(final ModifyTargetGroupType request);

  CheckedListenableFuture<ModifyTargetGroupAttributesResponseType> modifyTargetGroupAttributesAsync(final ModifyTargetGroupAttributesType request);

  CheckedListenableFuture<RegisterTargetsResponseType> registerTargetsAsync(final RegisterTargetsType request);

  CheckedListenableFuture<RemoveListenerCertificatesResponseType> removeListenerCertificatesAsync(final RemoveListenerCertificatesType request);

  CheckedListenableFuture<RemoveTagsResponseType> removeTagsAsync(final RemoveTagsType request);

  CheckedListenableFuture<SetIpAddressTypeResponseType> setIpAddressTypeAsync(final SetIpAddressTypeType request);

  CheckedListenableFuture<SetRulePrioritiesResponseType> setRulePrioritiesAsync(final SetRulePrioritiesType request);

  CheckedListenableFuture<SetSecurityGroupsResponseType> setSecurityGroupsAsync(final SetSecurityGroupsType request);

  CheckedListenableFuture<SetSubnetsResponseType> setSubnetsAsync(final SetSubnetsType request);

}
