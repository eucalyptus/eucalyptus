/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTargetHealthResult extends EucalyptusData {

  private TargetHealthDescriptions targetHealthDescriptions;

  public TargetHealthDescriptions getTargetHealthDescriptions() {
    return targetHealthDescriptions;
  }

  public void setTargetHealthDescriptions(final TargetHealthDescriptions targetHealthDescriptions) {
    this.targetHealthDescriptions = targetHealthDescriptions;
  }

}
