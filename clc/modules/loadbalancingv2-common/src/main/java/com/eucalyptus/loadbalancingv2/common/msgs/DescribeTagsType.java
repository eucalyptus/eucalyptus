/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class DescribeTagsType extends Loadbalancingv2Message {

  @Nonnull
  private ResourceArns resourceArns;

  public ResourceArns getResourceArns() {
    return resourceArns;
  }

  public void setResourceArns(final ResourceArns resourceArns) {
    this.resourceArns = resourceArns;
  }

}
