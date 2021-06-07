/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CopyDBClusterParameterGroupType extends RdsMessage {

  @Nonnull
  private String sourceDBClusterParameterGroupIdentifier;

  private TagList tags;

  @Nonnull
  private String targetDBClusterParameterGroupDescription;

  @Nonnull
  private String targetDBClusterParameterGroupIdentifier;

  public String getSourceDBClusterParameterGroupIdentifier() {
    return sourceDBClusterParameterGroupIdentifier;
  }

  public void setSourceDBClusterParameterGroupIdentifier(final String sourceDBClusterParameterGroupIdentifier) {
    this.sourceDBClusterParameterGroupIdentifier = sourceDBClusterParameterGroupIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getTargetDBClusterParameterGroupDescription() {
    return targetDBClusterParameterGroupDescription;
  }

  public void setTargetDBClusterParameterGroupDescription(final String targetDBClusterParameterGroupDescription) {
    this.targetDBClusterParameterGroupDescription = targetDBClusterParameterGroupDescription;
  }

  public String getTargetDBClusterParameterGroupIdentifier() {
    return targetDBClusterParameterGroupIdentifier;
  }

  public void setTargetDBClusterParameterGroupIdentifier(final String targetDBClusterParameterGroupIdentifier) {
    this.targetDBClusterParameterGroupIdentifier = targetDBClusterParameterGroupIdentifier;
  }

}
