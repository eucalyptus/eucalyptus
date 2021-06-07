/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterEndpointsType extends RdsMessage {

  private String dBClusterEndpointIdentifier;

  private String dBClusterIdentifier;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  public String getDBClusterEndpointIdentifier() {
    return dBClusterEndpointIdentifier;
  }

  public void setDBClusterEndpointIdentifier(final String dBClusterEndpointIdentifier) {
    this.dBClusterEndpointIdentifier = dBClusterEndpointIdentifier;
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
