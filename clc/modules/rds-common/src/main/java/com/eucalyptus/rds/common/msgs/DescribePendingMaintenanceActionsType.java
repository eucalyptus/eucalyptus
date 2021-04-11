/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribePendingMaintenanceActionsType extends RdsMessage {

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  private String resourceIdentifier;

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

  public String getResourceIdentifier() {
    return resourceIdentifier;
  }

  public void setResourceIdentifier(final String resourceIdentifier) {
    this.resourceIdentifier = resourceIdentifier;
  }

}
