/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.Date;
import java.util.List;
import org.immutables.value.Value;


@Value.Immutable
public interface LoadBalancerView {

  String getDisplayName();

  String getNaturalId();

  String getOwnerAccountNumber();

  default String getArn() {
    return String.format(
        "arn:aws:elasticloadbalancing::%1s:loadbalancer/%2s/%3s/%4s",
        getOwnerAccountNumber(),
        getType().getCode(),
        getDisplayName(),
        getNaturalId());
  }

  Date getCreationTimestamp();

  Date getLastUpdateTimestamp();

  LoadBalancer.Type getType();

  LoadBalancer.Scheme getScheme();

  LoadBalancer.State getState();

  LoadBalancer.IpAddressType getIpAddressType();

  String getCanonicalHostedZoneId();

  List<String> getSecurityGroupIds();

  List<LoadBalancerSubnetView> getSubnetViews();

  String getVpcId();

  default Option<LoadBalancerSubnetView> findSubnetView(final String availabilityZone) {
    return Stream.ofAll(getSubnetViews())
        .filter(view -> availabilityZone.equals(view.getAvailabilityZone()))
        .headOption();
  }
}
