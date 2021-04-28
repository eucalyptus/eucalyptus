/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ForwardActionConfig extends EucalyptusData {

  private TargetGroupStickinessConfig targetGroupStickinessConfig;

  private TargetGroupList targetGroups;

  public TargetGroupStickinessConfig getTargetGroupStickinessConfig() {
    return targetGroupStickinessConfig;
  }

  public void setTargetGroupStickinessConfig(final TargetGroupStickinessConfig targetGroupStickinessConfig) {
    this.targetGroupStickinessConfig = targetGroupStickinessConfig;
  }

  public TargetGroupList getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(final TargetGroupList targetGroups) {
    this.targetGroups = targetGroups;
  }

}
