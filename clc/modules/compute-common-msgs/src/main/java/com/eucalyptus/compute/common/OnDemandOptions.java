/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OnDemandOptions extends EucalyptusData {

  private String allocationStrategy;

  public String getAllocationStrategy( ) {
    return allocationStrategy;
  }

  public void setAllocationStrategy( final String allocationStrategy ) {
    this.allocationStrategy = allocationStrategy;
  }

}
