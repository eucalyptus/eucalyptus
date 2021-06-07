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


@ComponentPart(Loadbalancingv2.class)
public interface Loadbalancingv2Api {

  AddListenerCertificatesResponseType addListenerCertificates(final AddListenerCertificatesType request);

  AddTagsResponseType addTags(final AddTagsType request);

  CreateListenerResponseType createListener(final CreateListenerType request);

  CreateLoadBalancerResponseType createLoadBalancer(final CreateLoadBalancerType request);

  CreateRuleResponseType createRule(final CreateRuleType request);

  CreateTargetGroupResponseType createTargetGroup(final CreateTargetGroupType request);

  DeleteListenerResponseType deleteListener(final DeleteListenerType request);

  DeleteLoadBalancerResponseType deleteLoadBalancer(final DeleteLoadBalancerType request);

  DeleteRuleResponseType deleteRule(final DeleteRuleType request);

  DeleteTargetGroupResponseType deleteTargetGroup(final DeleteTargetGroupType request);

  DeregisterTargetsResponseType deregisterTargets(final DeregisterTargetsType request);

  DescribeAccountLimitsResponseType describeAccountLimits(final DescribeAccountLimitsType request);

  default DescribeAccountLimitsResponseType describeAccountLimits() {
    return describeAccountLimits(new DescribeAccountLimitsType());
  }

  DescribeListenerCertificatesResponseType describeListenerCertificates(final DescribeListenerCertificatesType request);

  DescribeListenersResponseType describeListeners(final DescribeListenersType request);

  default DescribeListenersResponseType describeListeners() {
    return describeListeners(new DescribeListenersType());
  }

  DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(final DescribeLoadBalancerAttributesType request);

  DescribeLoadBalancersResponseType describeLoadBalancers(final DescribeLoadBalancersType request);

  default DescribeLoadBalancersResponseType describeLoadBalancers() {
    return describeLoadBalancers(new DescribeLoadBalancersType());
  }

  DescribeRulesResponseType describeRules(final DescribeRulesType request);

  default DescribeRulesResponseType describeRules() {
    return describeRules(new DescribeRulesType());
  }

  DescribeSSLPoliciesResponseType describeSSLPolicies(final DescribeSSLPoliciesType request);

  default DescribeSSLPoliciesResponseType describeSSLPolicies() {
    return describeSSLPolicies(new DescribeSSLPoliciesType());
  }

  DescribeTagsResponseType describeTags(final DescribeTagsType request);

  DescribeTargetGroupAttributesResponseType describeTargetGroupAttributes(final DescribeTargetGroupAttributesType request);

  DescribeTargetGroupsResponseType describeTargetGroups(final DescribeTargetGroupsType request);

  default DescribeTargetGroupsResponseType describeTargetGroups() {
    return describeTargetGroups(new DescribeTargetGroupsType());
  }

  DescribeTargetHealthResponseType describeTargetHealth(final DescribeTargetHealthType request);

  ModifyListenerResponseType modifyListener(final ModifyListenerType request);

  ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(final ModifyLoadBalancerAttributesType request);

  ModifyRuleResponseType modifyRule(final ModifyRuleType request);

  ModifyTargetGroupResponseType modifyTargetGroup(final ModifyTargetGroupType request);

  ModifyTargetGroupAttributesResponseType modifyTargetGroupAttributes(final ModifyTargetGroupAttributesType request);

  RegisterTargetsResponseType registerTargets(final RegisterTargetsType request);

  RemoveListenerCertificatesResponseType removeListenerCertificates(final RemoveListenerCertificatesType request);

  RemoveTagsResponseType removeTags(final RemoveTagsType request);

  SetIpAddressTypeResponseType setIpAddressType(final SetIpAddressTypeType request);

  SetRulePrioritiesResponseType setRulePriorities(final SetRulePrioritiesType request);

  SetSecurityGroupsResponseType setSecurityGroups(final SetSecurityGroupsType request);

  SetSubnetsResponseType setSubnets(final SetSubnetsType request);

}
