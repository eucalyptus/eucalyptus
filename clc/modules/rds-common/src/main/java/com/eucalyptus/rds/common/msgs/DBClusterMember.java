/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterMember extends EucalyptusData {

  private String dBClusterParameterGroupStatus;

  private String dBInstanceIdentifier;

  private Boolean isClusterWriter;

  private Integer promotionTier;

  public String getDBClusterParameterGroupStatus() {
    return dBClusterParameterGroupStatus;
  }

  public void setDBClusterParameterGroupStatus(final String dBClusterParameterGroupStatus) {
    this.dBClusterParameterGroupStatus = dBClusterParameterGroupStatus;
  }

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public Boolean getIsClusterWriter() {
    return isClusterWriter;
  }

  public void setIsClusterWriter(final Boolean isClusterWriter) {
    this.isClusterWriter = isClusterWriter;
  }

  public Integer getPromotionTier() {
    return promotionTier;
  }

  public void setPromotionTier(final Integer promotionTier) {
    this.promotionTier = promotionTier;
  }

}
