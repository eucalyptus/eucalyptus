/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerListener.PROTOCOL;

@Immutable
public interface LoadBalancerListenerView {

  int getInstancePort();

  PROTOCOL getInstanceProtocol();

  int getLoadbalancerPort();

  PROTOCOL getProtocol();

  @Nullable
  String getCertificateId();
}
