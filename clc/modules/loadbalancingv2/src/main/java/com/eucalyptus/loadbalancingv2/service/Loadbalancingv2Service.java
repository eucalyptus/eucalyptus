/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.component.annotation.ComponentNamed;
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

/**
 *
 */
@ComponentNamed
public class Loadbalancingv2Service {

  public AddListenerCertificatesResponseType addListenerCertificates(final AddListenerCertificatesType request) {
    return request.getReply();
  }

  public AddTagsResponseType addTags(final AddTagsType request) {
    return request.getReply();
  }

  public CreateListenerResponseType createListener(final CreateListenerType request) {
    return request.getReply();
  }

  public CreateLoadBalancerResponseType createLoadBalancer(final CreateLoadBalancerType request) {
    return request.getReply();
  }

  public CreateRuleResponseType createRule(final CreateRuleType request) {
    return request.getReply();
  }

  public CreateTargetGroupResponseType createTargetGroup(final CreateTargetGroupType request) {
    return request.getReply();
  }

  public DeleteListenerResponseType deleteListener(final DeleteListenerType request) {
    return request.getReply();
  }

  public DeleteLoadBalancerResponseType deleteLoadBalancer(final DeleteLoadBalancerType request) {
    return request.getReply();
  }

  public DeleteRuleResponseType deleteRule(final DeleteRuleType request) {
    return request.getReply();
  }

  public DeleteTargetGroupResponseType deleteTargetGroup(final DeleteTargetGroupType request) {
    return request.getReply();
  }

  public DeregisterTargetsResponseType deregisterTargets(final DeregisterTargetsType request) {
    return request.getReply();
  }

  public DescribeAccountLimitsResponseType describeAccountLimits(final DescribeAccountLimitsType request) {
    return request.getReply();
  }

  public DescribeListenerCertificatesResponseType describeListenerCertificates(final DescribeListenerCertificatesType request) {
    return request.getReply();
  }

  public DescribeListenersResponseType describeListeners(final DescribeListenersType request) {
    return request.getReply();
  }

  public DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(final DescribeLoadBalancerAttributesType request) {
    return request.getReply();
  }

  public DescribeLoadBalancersResponseType describeLoadBalancers(final DescribeLoadBalancersType request) {
    return request.getReply();
  }

  public DescribeRulesResponseType describeRules(final DescribeRulesType request) {
    return request.getReply();
  }

  public DescribeSSLPoliciesResponseType describeSSLPolicies(final DescribeSSLPoliciesType request) {
    return request.getReply();
  }

  public DescribeTagsResponseType describeTags(final DescribeTagsType request) {
    return request.getReply();
  }

  public DescribeTargetGroupAttributesResponseType describeTargetGroupAttributes(final DescribeTargetGroupAttributesType request) {
    return request.getReply();
  }

  public DescribeTargetGroupsResponseType describeTargetGroups(final DescribeTargetGroupsType request) {
    return request.getReply();
  }

  public DescribeTargetHealthResponseType describeTargetHealth(final DescribeTargetHealthType request) {
    return request.getReply();
  }

  public ModifyListenerResponseType modifyListener(final ModifyListenerType request) {
    return request.getReply();
  }

  public ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(final ModifyLoadBalancerAttributesType request) {
    return request.getReply();
  }

  public ModifyRuleResponseType modifyRule(final ModifyRuleType request) {
    return request.getReply();
  }

  public ModifyTargetGroupResponseType modifyTargetGroup(final ModifyTargetGroupType request) {
    return request.getReply();
  }

  public ModifyTargetGroupAttributesResponseType modifyTargetGroupAttributes(final ModifyTargetGroupAttributesType request) {
    return request.getReply();
  }

  public RegisterTargetsResponseType registerTargets(final RegisterTargetsType request) {
    return request.getReply();
  }

  public RemoveListenerCertificatesResponseType removeListenerCertificates(final RemoveListenerCertificatesType request) {
    return request.getReply();
  }

  public RemoveTagsResponseType removeTags(final RemoveTagsType request) {
    return request.getReply();
  }

  public SetIpAddressTypeResponseType setIpAddressType(final SetIpAddressTypeType request) {
    return request.getReply();
  }

  public SetRulePrioritiesResponseType setRulePriorities(final SetRulePrioritiesType request) {
    return request.getReply();
  }

  public SetSecurityGroupsResponseType setSecurityGroups(final SetSecurityGroupsType request) {
    return request.getReply();
  }

  public SetSubnetsResponseType setSubnets(final SetSubnetsType request) {
    return request.getReply();
  }

}
