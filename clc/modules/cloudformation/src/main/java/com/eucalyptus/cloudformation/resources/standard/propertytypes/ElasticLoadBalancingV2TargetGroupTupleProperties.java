/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2TargetGroupTupleProperties {

  @Property
  private String targetGroupArn;

  @Property
  private Integer weight;

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("targetGroupArn", targetGroupArn)
        .add("weight", weight)
        .toString();
  }
}
