/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateSpotMarketOptions extends EucalyptusData {

  private Integer blockDurationMinutes;
  private String instanceInterruptionBehavior;
  private String maxPrice;
  private String spotInstanceType;
  private java.util.Date validUntil;

  public Integer getBlockDurationMinutes( ) {
    return blockDurationMinutes;
  }

  public void setBlockDurationMinutes( final Integer blockDurationMinutes ) {
    this.blockDurationMinutes = blockDurationMinutes;
  }

  public String getInstanceInterruptionBehavior( ) {
    return instanceInterruptionBehavior;
  }

  public void setInstanceInterruptionBehavior( final String instanceInterruptionBehavior ) {
    this.instanceInterruptionBehavior = instanceInterruptionBehavior;
  }

  public String getMaxPrice( ) {
    return maxPrice;
  }

  public void setMaxPrice( final String maxPrice ) {
    this.maxPrice = maxPrice;
  }

  public String getSpotInstanceType( ) {
    return spotInstanceType;
  }

  public void setSpotInstanceType( final String spotInstanceType ) {
    this.spotInstanceType = spotInstanceType;
  }

  public java.util.Date getValidUntil( ) {
    return validUntil;
  }

  public void setValidUntil( final java.util.Date validUntil ) {
    this.validUntil = validUntil;
  }

}
