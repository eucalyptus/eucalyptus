/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBClusterParameterGroupType extends RdsMessage {

  @Nonnull
  private String dBClusterParameterGroupName;

  @Nonnull
  private String dBParameterGroupFamily;

  @Nonnull
  private String description;

  private TagList tags;

  public String getDBClusterParameterGroupName() {
    return dBClusterParameterGroupName;
  }

  public void setDBClusterParameterGroupName(final String dBClusterParameterGroupName) {
    this.dBClusterParameterGroupName = dBClusterParameterGroupName;
  }

  public String getDBParameterGroupFamily() {
    return dBParameterGroupFamily;
  }

  public void setDBParameterGroupFamily(final String dBParameterGroupFamily) {
    this.dBParameterGroupFamily = dBParameterGroupFamily;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
