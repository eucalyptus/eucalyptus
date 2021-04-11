/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeInstallationMediaType extends RdsMessage {

  private FilterList filters;

  private String installationMediaId;

  private String marker;

  private Integer maxRecords;

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public String getInstallationMediaId() {
    return installationMediaId;
  }

  public void setInstallationMediaId(final String installationMediaId) {
    this.installationMediaId = installationMediaId;
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
