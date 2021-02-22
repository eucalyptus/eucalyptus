/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class RemoveTagsFromResourceType extends RdsMessage {

  @Nonnull
  private String resourceName;

  @Nonnull
  private KeyList tagKeys;

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(final String resourceName) {
    this.resourceName = resourceName;
  }

  public KeyList getTagKeys() {
    return tagKeys;
  }

  public void setTagKeys(final KeyList tagKeys) {
    this.tagKeys = tagKeys;
  }

}
