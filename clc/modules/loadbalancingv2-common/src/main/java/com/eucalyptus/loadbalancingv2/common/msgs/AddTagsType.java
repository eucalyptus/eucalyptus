/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class AddTagsType extends Loadbalancingv2Message {

  @Nonnull
  private ResourceArns resourceArns;

  @Nonnull
  @FieldRange(min = 1)
  private TagList tags;

  public ResourceArns getResourceArns() {
    return resourceArns;
  }

  public void setResourceArns(final ResourceArns resourceArns) {
    this.resourceArns = resourceArns;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
