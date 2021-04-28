/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTargetGroupAttributesResult extends EucalyptusData {

  private TargetGroupAttributes attributes;

  public TargetGroupAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(final TargetGroupAttributes attributes) {
    this.attributes = attributes;
  }

}
