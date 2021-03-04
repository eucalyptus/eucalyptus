/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetGroupTuple extends EucalyptusData {

  private String targetGroupArn;

  private Integer weight;

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(final Integer weight) {
    this.weight = weight;
  }

}
