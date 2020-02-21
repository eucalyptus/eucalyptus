/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.config;

import org.apache.log4j.Logger;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.route53.common.Route53;

/**
 *
 */
@ComponentPart(Route53.class)
public class Route53ServiceBuilder extends AbstractServiceBuilder<Route53Configuration> {

  private static final Logger LOG = Logger.getLogger(Route53ServiceBuilder.class);

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Route53.class);
  }

  @Override
  public Route53Configuration newInstance(String partition, String name, String host, Integer port) {
    return new Route53Configuration(partition, name, host, port);
  }

  @Override
  public Route53Configuration newInstance() {
    return new Route53Configuration();
  }
}
