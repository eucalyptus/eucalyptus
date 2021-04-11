/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBSecurityGroupType extends RdsMessage {

  @Nonnull
  private String dBSecurityGroupDescription;

  @Nonnull
  private String dBSecurityGroupName;

  private TagList tags;

  public String getDBSecurityGroupDescription() {
    return dBSecurityGroupDescription;
  }

  public void setDBSecurityGroupDescription(final String dBSecurityGroupDescription) {
    this.dBSecurityGroupDescription = dBSecurityGroupDescription;
  }

  public String getDBSecurityGroupName() {
    return dBSecurityGroupName;
  }

  public void setDBSecurityGroupName(final String dBSecurityGroupName) {
    this.dBSecurityGroupName = dBSecurityGroupName;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
