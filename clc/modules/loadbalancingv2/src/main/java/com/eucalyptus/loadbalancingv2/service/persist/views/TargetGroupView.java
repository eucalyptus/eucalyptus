/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import com.eucalyptus.loadbalancingv2.service.persist.entities.TargetGroup;
import javax.annotation.Nullable;
import org.immutables.value.Value;


@Value.Immutable
public interface TargetGroupView {

  String getDisplayName();

  String getNaturalId();

  String getOwnerAccountNumber();

  default String getArn() {
    return String.format(
        "arn:aws:elasticloadbalancing::%1s:targetgroup/%2s/%3s",
        getOwnerAccountNumber(),
        getDisplayName(),
        getNaturalId());
  }

  TargetGroup.TargetType getTargetType();

  @Nullable
  TargetGroup.Protocol getProtocol();

  @Nullable
  TargetGroup.ProtocolVersion getProtocolVersion();

  @Nullable
  Integer getPort();

  @Nullable
  String getMatcherGrpcCode();

  @Nullable
  String getMatcherHttpCode();

  @Nullable
  String getVpcId();

  @Nullable
  Boolean getHealthCheckEnabled();

  @Nullable
  Integer getHealthCheckIntervalSeconds();

  @Nullable
  String getHealthCheckPath();

  @Nullable
  Integer getHealthCheckPort();

  @Nullable
  TargetGroup.Protocol getHealthCheckProtocol();

  @Nullable
  Integer getHealthCheckTimeoutSeconds();

  @Nullable
  Integer getHealthyThresholdCount();

  @Nullable
  Integer getUnhealthyThresholdCount();

}
