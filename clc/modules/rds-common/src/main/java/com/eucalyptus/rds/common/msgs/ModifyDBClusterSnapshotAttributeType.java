/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBClusterSnapshotAttributeType extends RdsMessage {

  @Nonnull
  private String attributeName;

  @Nonnull
  private String dBClusterSnapshotIdentifier;

  private AttributeValueList valuesToAdd;

  private AttributeValueList valuesToRemove;

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(final String attributeName) {
    this.attributeName = attributeName;
  }

  public String getDBClusterSnapshotIdentifier() {
    return dBClusterSnapshotIdentifier;
  }

  public void setDBClusterSnapshotIdentifier(final String dBClusterSnapshotIdentifier) {
    this.dBClusterSnapshotIdentifier = dBClusterSnapshotIdentifier;
  }

  public AttributeValueList getValuesToAdd() {
    return valuesToAdd;
  }

  public void setValuesToAdd(final AttributeValueList valuesToAdd) {
    this.valuesToAdd = valuesToAdd;
  }

  public AttributeValueList getValuesToRemove() {
    return valuesToRemove;
  }

  public void setValuesToRemove(final AttributeValueList valuesToRemove) {
    this.valuesToRemove = valuesToRemove;
  }

}
