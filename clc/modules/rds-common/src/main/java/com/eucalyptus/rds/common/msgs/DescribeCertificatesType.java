/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeCertificatesType extends RdsMessage {

  private String certificateIdentifier;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  public String getCertificateIdentifier() {
    return certificateIdentifier;
  }

  public void setCertificateIdentifier(final String certificateIdentifier) {
    this.certificateIdentifier = certificateIdentifier;
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
