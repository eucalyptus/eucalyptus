/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBProxiesResult extends EucalyptusData {

  private DBProxyList dBProxies;

  private String marker;

  public DBProxyList getDBProxies() {
    return dBProxies;
  }

  public void setDBProxies(final DBProxyList dBProxies) {
    this.dBProxies = dBProxies;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
