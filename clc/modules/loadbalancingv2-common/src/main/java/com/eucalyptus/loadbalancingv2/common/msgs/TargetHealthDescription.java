/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetHealthDescription extends EucalyptusData {

  private String healthCheckPort;

  private TargetDescription target;

  private TargetHealth targetHealth;

  public String getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(final String healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  public TargetDescription getTarget() {
    return target;
  }

  public void setTarget(final TargetDescription target) {
    this.target = target;
  }

  public TargetHealth getTargetHealth() {
    return targetHealth;
  }

  public void setTargetHealth(final TargetHealth targetHealth) {
    this.targetHealth = targetHealth;
  }

}
