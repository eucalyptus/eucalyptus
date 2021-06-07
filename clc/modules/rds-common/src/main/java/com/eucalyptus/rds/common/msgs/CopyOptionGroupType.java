/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CopyOptionGroupType extends RdsMessage {

  @Nonnull
  private String sourceOptionGroupIdentifier;

  private TagList tags;

  @Nonnull
  private String targetOptionGroupDescription;

  @Nonnull
  private String targetOptionGroupIdentifier;

  public String getSourceOptionGroupIdentifier() {
    return sourceOptionGroupIdentifier;
  }

  public void setSourceOptionGroupIdentifier(final String sourceOptionGroupIdentifier) {
    this.sourceOptionGroupIdentifier = sourceOptionGroupIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getTargetOptionGroupDescription() {
    return targetOptionGroupDescription;
  }

  public void setTargetOptionGroupDescription(final String targetOptionGroupDescription) {
    this.targetOptionGroupDescription = targetOptionGroupDescription;
  }

  public String getTargetOptionGroupIdentifier() {
    return targetOptionGroupIdentifier;
  }

  public void setTargetOptionGroupIdentifier(final String targetOptionGroupIdentifier) {
    this.targetOptionGroupIdentifier = targetOptionGroupIdentifier;
  }

}
