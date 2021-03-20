/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.servo;

import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableServoView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ServoView;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import java.util.concurrent.ConcurrentMap;

public class ServoCache {

  private static final ConcurrentMap<String, ServoView> servosByInstanceId =
      Maps.newConcurrentMap();
  private static final ConcurrentMap<String, ServoView> servosByLoadBalancerId =
      Maps.newConcurrentMap();
  private static final ConcurrentMap<Tuple2<String,String>, ServoView> servosByLoadBalancerName =
      Maps.newConcurrentMap();

  public static void notifyServo(
      final String instanceId,
      final String loadBalancerId,
      final String loadBalancerName,
      final String ownerAccountNumber,
      final String availabilityZone,
      final String ipAddress
  ){
    final ServoView servoView = ImmutableServoView.builder()
        .displayName(instanceId)
        .loadBalancerId(loadBalancerId)
        .loadBalancerName(loadBalancerName)
        .ownerAccountNumber(ownerAccountNumber)
        .availabiltyZone(availabilityZone)
        .ipAddress(ipAddress)
        .build();

    servosByInstanceId.put(instanceId, servoView);
    servosByLoadBalancerId.put(loadBalancerId, servoView);
    final ServoView previousServoView = servosByLoadBalancerName.put(
        key(ownerAccountNumber, loadBalancerName), servoView);
    if (previousServoView != null &&
        !previousServoView.getDisplayName().equals(servoView.getDisplayName())) {
      // servo instance replaced
      servosByInstanceId.remove(previousServoView.getDisplayName());
    }
  }

  public static Iterable<ServoView> servosForInstance(final String instanceId) {
    return Option.of(servosByInstanceId.get(instanceId));
  }

  public static Iterable<ServoView> servosForLoadBalancer(final String loadBalancerId) {
    return Option.of(servosByLoadBalancerId.get(loadBalancerId));
  }

  public static Iterable<ServoView> servosForLoadBalancer(
      final String accountNumber,
      final String name
  ) {
    return Option.of(servosByLoadBalancerName.get(key(accountNumber, name)));
  }

  private static Tuple2<String,String> key(final String accountNumber, final String name) {
    return Tuple.of(accountNumber, name.toLowerCase());
  }
}
