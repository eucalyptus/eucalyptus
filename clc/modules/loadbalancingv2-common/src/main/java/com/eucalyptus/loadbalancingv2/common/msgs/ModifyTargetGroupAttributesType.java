/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class ModifyTargetGroupAttributesType extends Loadbalancingv2Message {

  @Nonnull
  private TargetGroupAttributes attributes;

  @Nonnull
  private String targetGroupArn;

  public TargetGroupAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(final TargetGroupAttributes attributes) {
    this.attributes = attributes;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

}
