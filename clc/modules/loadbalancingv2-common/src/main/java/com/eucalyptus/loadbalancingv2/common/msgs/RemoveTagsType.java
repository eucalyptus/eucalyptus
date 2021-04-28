/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class RemoveTagsType extends Loadbalancingv2Message {

  @Nonnull
  private ResourceArns resourceArns;

  @Nonnull
  private TagKeys tagKeys;

  public ResourceArns getResourceArns() {
    return resourceArns;
  }

  public void setResourceArns(final ResourceArns resourceArns) {
    this.resourceArns = resourceArns;
  }

  public TagKeys getTagKeys() {
    return tagKeys;
  }

  public void setTagKeys(final TagKeys tagKeys) {
    this.tagKeys = tagKeys;
  }

}
