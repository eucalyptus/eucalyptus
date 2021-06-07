/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ValidStorageOptions extends EucalyptusData {

  private DoubleRangeList iopsToStorageRatio;

  private RangeList provisionedIops;

  private RangeList storageSize;

  private String storageType;

  private Boolean supportsStorageAutoscaling;

  public DoubleRangeList getIopsToStorageRatio() {
    return iopsToStorageRatio;
  }

  public void setIopsToStorageRatio(final DoubleRangeList iopsToStorageRatio) {
    this.iopsToStorageRatio = iopsToStorageRatio;
  }

  public RangeList getProvisionedIops() {
    return provisionedIops;
  }

  public void setProvisionedIops(final RangeList provisionedIops) {
    this.provisionedIops = provisionedIops;
  }

  public RangeList getStorageSize() {
    return storageSize;
  }

  public void setStorageSize(final RangeList storageSize) {
    this.storageSize = storageSize;
  }

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(final String storageType) {
    this.storageType = storageType;
  }

  public Boolean getSupportsStorageAutoscaling() {
    return supportsStorageAutoscaling;
  }

  public void setSupportsStorageAutoscaling(final Boolean supportsStorageAutoscaling) {
    this.supportsStorageAutoscaling = supportsStorageAutoscaling;
  }

}
