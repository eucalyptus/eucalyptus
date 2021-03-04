/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class DescribeTargetHealthType extends Loadbalancingv2Message {

  @Nonnull
  private String targetGroupArn;

  private TargetDescriptions targets;

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public TargetDescriptions getTargets() {
    return targets;
  }

  public void setTargets(final TargetDescriptions targets) {
    this.targets = targets;
  }

}
