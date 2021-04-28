/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.config;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;

/**
 *
 */
@Entity
@PersistenceContext(name = "eucalyptus_config")
@ComponentPart(Loadbalancingv2.class)
public class Loadbalancingv2Configuration extends ComponentConfiguration implements Serializable {

  public static final String SERVICE_PATH = "/services/Loadbalancingv2";

  private static final long serialVersionUID = 1L;

  public Loadbalancingv2Configuration() {
  }

  public Loadbalancingv2Configuration(String partition, String name, String hostName, Integer port) {
    super(partition, name, hostName, port, SERVICE_PATH);
  }
}
