/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TagDescription extends EucalyptusData {

  private String resourceArn;

  private TagList tags;

  public String getResourceArn() {
    return resourceArn;
  }

  public void setResourceArn(final String resourceArn) {
    this.resourceArn = resourceArn;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
