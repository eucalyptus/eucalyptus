/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendInstance.STATE;

@Immutable
public interface LoadBalancerBackendInstanceView {

  String getDisplayName();

  STATE getState();

  String getInstanceId();

  STATE getBackendState();

  String getReasonCode();

  String getDescription();

  @Nullable
  String getIpAddress();

  @Nullable
  String getPartition();

  Date getInstanceUpdateTimestamp();

  Date getCreationTimestamp();
}
