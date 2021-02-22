/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBSecurityGroupsType extends RdsMessage {

  private String dBSecurityGroupName;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  public String getDBSecurityGroupName() {
    return dBSecurityGroupName;
  }

  public void setDBSecurityGroupName(final String dBSecurityGroupName) {
    this.dBSecurityGroupName = dBSecurityGroupName;
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
