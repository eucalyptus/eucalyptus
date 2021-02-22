/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterSnapshotAttribute extends EucalyptusData {

  private String attributeName;

  private AttributeValueList attributeValues;

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(final String attributeName) {
    this.attributeName = attributeName;
  }

  public AttributeValueList getAttributeValues() {
    return attributeValues;
  }

  public void setAttributeValues(final AttributeValueList attributeValues) {
    this.attributeValues = attributeValues;
  }

}
