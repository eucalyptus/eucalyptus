/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBSubnetGroupType extends RdsMessage {

  @Nonnull
  private String dBSubnetGroupDescription;

  @Nonnull
  private String dBSubnetGroupName;

  @Nonnull
  private SubnetIdentifierList subnetIds;

  private TagList tags;

  public String getDBSubnetGroupDescription() {
    return dBSubnetGroupDescription;
  }

  public void setDBSubnetGroupDescription(final String dBSubnetGroupDescription) {
    this.dBSubnetGroupDescription = dBSubnetGroupDescription;
  }

  public String getDBSubnetGroupName() {
    return dBSubnetGroupName;
  }

  public void setDBSubnetGroupName(final String dBSubnetGroupName) {
    this.dBSubnetGroupName = dBSubnetGroupName;
  }

  public SubnetIdentifierList getSubnetIds() {
    return subnetIds;
  }

  public void setSubnetIds(final SubnetIdentifierList subnetIds) {
    this.subnetIds = subnetIds;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
