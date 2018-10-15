/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SpotOptions extends EucalyptusData {

  private String allocationStrategy;
  private String instanceInterruptionBehavior;
  private Integer instancePoolsToUseCount;

  public String getAllocationStrategy( ) {
    return allocationStrategy;
  }

  public void setAllocationStrategy( final String allocationStrategy ) {
    this.allocationStrategy = allocationStrategy;
  }

  public String getInstanceInterruptionBehavior( ) {
    return instanceInterruptionBehavior;
  }

  public void setInstanceInterruptionBehavior( final String instanceInterruptionBehavior ) {
    this.instanceInterruptionBehavior = instanceInterruptionBehavior;
  }

  public Integer getInstancePoolsToUseCount( ) {
    return instancePoolsToUseCount;
  }

  public void setInstancePoolsToUseCount( final Integer instancePoolsToUseCount ) {
    this.instancePoolsToUseCount = instancePoolsToUseCount;
  }

}
