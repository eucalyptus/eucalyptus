/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEventCategoriesType extends RdsMessage {

  private FilterList filters;

  private String sourceType;

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(final String sourceType) {
    this.sourceType = sourceType;
  }

}
