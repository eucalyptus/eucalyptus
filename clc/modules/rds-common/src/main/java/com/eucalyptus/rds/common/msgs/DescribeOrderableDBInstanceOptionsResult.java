/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeOrderableDBInstanceOptionsResult extends EucalyptusData {

  private String marker;

  private OrderableDBInstanceOptionsList orderableDBInstanceOptions;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public OrderableDBInstanceOptionsList getOrderableDBInstanceOptions() {
    return orderableDBInstanceOptions;
  }

  public void setOrderableDBInstanceOptions(final OrderableDBInstanceOptionsList orderableDBInstanceOptions) {
    this.orderableDBInstanceOptions = orderableDBInstanceOptions;
  }

}
