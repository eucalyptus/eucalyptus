/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class ElasticLoadBalancingV2ForwardConfigProperties {

  @Property
  private ArrayList<ElasticLoadBalancingV2TargetGroupTupleProperties> targetGroups =
      Lists.newArrayList( );

  @Property
  private ElasticLoadBalancingV2TargetGroupStickinessConfigProperties targetGroupStickinessConfig;

  public ArrayList<ElasticLoadBalancingV2TargetGroupTupleProperties> getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(
      ArrayList<ElasticLoadBalancingV2TargetGroupTupleProperties> targetGroups) {
    this.targetGroups = targetGroups;
  }

  public ElasticLoadBalancingV2TargetGroupStickinessConfigProperties getTargetGroupStickinessConfig() {
    return targetGroupStickinessConfig;
  }

  public void setTargetGroupStickinessConfig(
      ElasticLoadBalancingV2TargetGroupStickinessConfigProperties targetGroupStickinessConfig) {
    this.targetGroupStickinessConfig = targetGroupStickinessConfig;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("targetGroups", targetGroups)
        .add("targetGroupStickinessConfig", targetGroupStickinessConfig)
        .toString();
  }
}
