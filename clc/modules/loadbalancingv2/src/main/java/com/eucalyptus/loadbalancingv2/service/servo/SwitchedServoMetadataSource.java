/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.servo;

import com.eucalyptus.loadbalancing.LoadBalancerHelper;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.util.Exceptions;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class SwitchedServoMetadataSource implements LoadBalancerHelper.ServoMetadataSource {

  private final LoadBalancerHelper.ServoMetadataSource classic = new LoadBalancerHelper.ClassicServoMetadataSource();
  private final LoadBalancerHelper.ServoMetadataSource regular = new Loadbalancingv2ServoMetadataSource();

  @Override
  public List<String> listPoliciesForLoadBalancer(
      final String accountNumber,
      final String loadBalancerNameOrArn
  ) {
    return getSource(loadBalancerNameOrArn)
        .listPoliciesForLoadBalancer(accountNumber, loadBalancerNameOrArn);
  }

  @Override
  public PolicyDescription getLoadBalancerPolicy(
      final String accountNumber,
      final String loadBalancerNameOrArn,
      final String policyName
  ) {
    return getSource(loadBalancerNameOrArn)
        .getLoadBalancerPolicy(accountNumber, loadBalancerNameOrArn, policyName);
  }

  @Override
  public Map<String, LoadBalancerServoDescription> getLoadBalancerServoDescriptions(
      final String accountNumber,
      final String loadBalancerNameOrArn
  ) {
    return getSource(loadBalancerNameOrArn)
        .getLoadBalancerServoDescriptions(accountNumber, loadBalancerNameOrArn);
  }

  @Override
  public List<String> getServoInstances(
      final String accountNumber,
      final String loadBalancerNameOrArn
  ) {
    return getSource(loadBalancerNameOrArn)
        .getServoInstances(accountNumber, loadBalancerNameOrArn);
  }

  @Override
  public HealthCheck getLoadBalancerHealthCheck(
      final String accountNumber,
      final String loadBalancerNameOrArn
  ) {
    return getSource(loadBalancerNameOrArn)
        .getLoadBalancerHealthCheck(accountNumber, loadBalancerNameOrArn);
  }

  @Override
  public Set<String> resolveIpsForLoadBalancer(
      final String accountNumber,
      final String loadBalancerName) {
    Set<String> ips = regular.resolveIpsForLoadBalancer(accountNumber, loadBalancerName);
    if (ips.isEmpty()) {
      try {
        ips = classic.resolveIpsForLoadBalancer(accountNumber, loadBalancerName);
      } catch (Exception e) {
        if (!Exceptions.isCausedBy(e, NoSuchElementException.class)) {
          throw e;
        }
      }
    }
    return ips;
  }

  @Override
  public Map<String, String> filterInstanceStatus(
      final String accountNumber,
      final String loadBalancerNameOrArn,
      final String servoInstanceId,
      final Map<String, String> statusMap) {
    return getSource(loadBalancerNameOrArn)
        .filterInstanceStatus(accountNumber, loadBalancerNameOrArn, servoInstanceId, statusMap);
  }

  @Override
  public void notifyBackendInstanceStatus(
      final String accountNumber,
      final String loadBalancerNameOrArn,
      final Map<String, String> statusMap
  ) {
    getSource(loadBalancerNameOrArn)
        .notifyBackendInstanceStatus(accountNumber, loadBalancerNameOrArn, statusMap);
  }

  @Override
  public void notifyServoInstanceFailure(
      final String instanceId
  ) {
    try {
      classic.notifyServoInstanceFailure(instanceId);
    } catch (Exception e) {
      if (!Exceptions.isCausedBy(e, NoSuchElementException.class)) {
        throw e;
      }
    }
    regular.notifyServoInstanceFailure(instanceId);
  }

  private LoadBalancerHelper.ServoMetadataSource getSource(final String loadBalancerNameOrArn) {
    if (loadBalancerNameOrArn == null || !loadBalancerNameOrArn.startsWith("arn:")) {
      return classic;
    } else {
      return regular;
    }
  }
}
