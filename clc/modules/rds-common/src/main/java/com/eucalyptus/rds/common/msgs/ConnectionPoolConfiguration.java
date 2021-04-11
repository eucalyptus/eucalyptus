/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ConnectionPoolConfiguration extends EucalyptusData {

  private Integer connectionBorrowTimeout;

  private String initQuery;

  private Integer maxConnectionsPercent;

  private Integer maxIdleConnectionsPercent;

  private StringList sessionPinningFilters;

  public Integer getConnectionBorrowTimeout() {
    return connectionBorrowTimeout;
  }

  public void setConnectionBorrowTimeout(final Integer connectionBorrowTimeout) {
    this.connectionBorrowTimeout = connectionBorrowTimeout;
  }

  public String getInitQuery() {
    return initQuery;
  }

  public void setInitQuery(final String initQuery) {
    this.initQuery = initQuery;
  }

  public Integer getMaxConnectionsPercent() {
    return maxConnectionsPercent;
  }

  public void setMaxConnectionsPercent(final Integer maxConnectionsPercent) {
    this.maxConnectionsPercent = maxConnectionsPercent;
  }

  public Integer getMaxIdleConnectionsPercent() {
    return maxIdleConnectionsPercent;
  }

  public void setMaxIdleConnectionsPercent(final Integer maxIdleConnectionsPercent) {
    this.maxIdleConnectionsPercent = maxIdleConnectionsPercent;
  }

  public StringList getSessionPinningFilters() {
    return sessionPinningFilters;
  }

  public void setSessionPinningFilters(final StringList sessionPinningFilters) {
    this.sessionPinningFilters = sessionPinningFilters;
  }

}
