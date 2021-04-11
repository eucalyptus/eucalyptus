/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeReservedDBInstancesOfferingsType extends RdsMessage {

  private String dBInstanceClass;

  private String duration;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  private Boolean multiAZ;

  private String offeringType;

  private String productDescription;

  private String reservedDBInstancesOfferingId;

  public String getDBInstanceClass() {
    return dBInstanceClass;
  }

  public void setDBInstanceClass(final String dBInstanceClass) {
    this.dBInstanceClass = dBInstanceClass;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(final String duration) {
    this.duration = duration;
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

  public Boolean getMultiAZ() {
    return multiAZ;
  }

  public void setMultiAZ(final Boolean multiAZ) {
    this.multiAZ = multiAZ;
  }

  public String getOfferingType() {
    return offeringType;
  }

  public void setOfferingType(final String offeringType) {
    this.offeringType = offeringType;
  }

  public String getProductDescription() {
    return productDescription;
  }

  public void setProductDescription(final String productDescription) {
    this.productDescription = productDescription;
  }

  public String getReservedDBInstancesOfferingId() {
    return reservedDBInstancesOfferingId;
  }

  public void setReservedDBInstancesOfferingId(final String reservedDBInstancesOfferingId) {
    this.reservedDBInstancesOfferingId = reservedDBInstancesOfferingId;
  }

}
