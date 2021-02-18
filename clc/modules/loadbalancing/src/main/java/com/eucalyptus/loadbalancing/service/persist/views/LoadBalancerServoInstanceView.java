/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerServoInstance.DNS_STATE;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerServoInstance.STATE;

@Immutable
public interface LoadBalancerServoInstanceView {

  String getInstanceId();

  STATE getState();

  DNS_STATE getDnsState();

  @Nullable
  String getAddress();

  @Nullable
  String getPrivateIp();

  Date getCertificateExpiration();

  boolean isCertificateExpired();

  default boolean canResolveDns() {
    return
        DNS_STATE.Registered.equals(getDnsState()) &&
            STATE.InService.equals(getState());
  }
}
