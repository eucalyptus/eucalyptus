/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.views;

import com.eucalyptus.loadbalancingv2.service.persist.entities.Target;
import javax.annotation.Nullable;

public interface TargetView {

  String getTargetId();

  @Nullable
  String getAvailabilityZone();

  String getIpAddress();

  @Nullable
  Integer getPort();

  @Nullable
  Integer getHealthCheckPort();

  @Nullable
  String getTargetHealthDescription();

  @Nullable
  String getTargetHealthReason();

  @Nullable
  Target.State getTargetHealthState();
}
