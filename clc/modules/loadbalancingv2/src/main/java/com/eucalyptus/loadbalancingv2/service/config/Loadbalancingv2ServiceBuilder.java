/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.config;

import org.apache.log4j.Logger;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;

/**
 *
 */
@ComponentPart(Loadbalancingv2.class)
public class Loadbalancingv2ServiceBuilder extends AbstractServiceBuilder<Loadbalancingv2Configuration> {

  private static final Logger LOG = Logger.getLogger(Loadbalancingv2ServiceBuilder.class);

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Loadbalancingv2.class);
  }

  @Override
  public Loadbalancingv2Configuration newInstance(String partition, String name, String host, Integer port) {
    return new Loadbalancingv2Configuration(partition, name, host, port);
  }

  @Override
  public Loadbalancingv2Configuration newInstance() {
    return new Loadbalancingv2Configuration();
  }
}
