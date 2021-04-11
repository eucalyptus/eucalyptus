/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DescribeDBClusterBacktracksType extends RdsMessage {

  private String backtrackIdentifier;

  @Nonnull
  private String dBClusterIdentifier;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  public String getBacktrackIdentifier() {
    return backtrackIdentifier;
  }

  public void setBacktrackIdentifier(final String backtrackIdentifier) {
    this.backtrackIdentifier = backtrackIdentifier;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(final Integer maxRecords) {
    this.maxRecords = maxRecords;
  }

}
