/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SpotOptionsRequest extends EucalyptusData {

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
