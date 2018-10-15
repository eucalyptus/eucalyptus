/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OnDemandOptionsRequest extends EucalyptusData {

  private String allocationStrategy;

  public String getAllocationStrategy( ) {
    return allocationStrategy;
  }

  public void setAllocationStrategy( final String allocationStrategy ) {
    this.allocationStrategy = allocationStrategy;
  }

}
