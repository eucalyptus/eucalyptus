/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.config;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.route53.common.Route53;

/**
 *
 */
@Entity
@PersistenceContext(name = "eucalyptus_config")
@ComponentPart(Route53.class)
public class Route53Configuration extends ComponentConfiguration implements Serializable {

  public static final String SERVICE_PATH = "/services/Route53";

  private static final long serialVersionUID = 1L;

  public Route53Configuration() {
  }

  public Route53Configuration(String partition, String name, String hostName, Integer port) {
    super(partition, name, hostName, port, SERVICE_PATH);
  }
}
