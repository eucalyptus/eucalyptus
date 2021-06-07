/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CopyDBParameterGroupType extends RdsMessage {

  @Nonnull
  private String sourceDBParameterGroupIdentifier;

  private TagList tags;

  @Nonnull
  private String targetDBParameterGroupDescription;

  @Nonnull
  private String targetDBParameterGroupIdentifier;

  public String getSourceDBParameterGroupIdentifier() {
    return sourceDBParameterGroupIdentifier;
  }

  public void setSourceDBParameterGroupIdentifier(final String sourceDBParameterGroupIdentifier) {
    this.sourceDBParameterGroupIdentifier = sourceDBParameterGroupIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getTargetDBParameterGroupDescription() {
    return targetDBParameterGroupDescription;
  }

  public void setTargetDBParameterGroupDescription(final String targetDBParameterGroupDescription) {
    this.targetDBParameterGroupDescription = targetDBParameterGroupDescription;
  }

  public String getTargetDBParameterGroupIdentifier() {
    return targetDBParameterGroupIdentifier;
  }

  public void setTargetDBParameterGroupIdentifier(final String targetDBParameterGroupIdentifier) {
    this.targetDBParameterGroupIdentifier = targetDBParameterGroupIdentifier;
  }

}
