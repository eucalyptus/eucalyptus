/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ReservedDBInstancesOffering extends EucalyptusData {

  private String currencyCode;

  private String dBInstanceClass;

  private Integer duration;

  private Double fixedPrice;

  private Boolean multiAZ;

  private String offeringType;

  private String productDescription;

  private RecurringChargeList recurringCharges;

  private String reservedDBInstancesOfferingId;

  private Double usagePrice;

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(final String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public String getDBInstanceClass() {
    return dBInstanceClass;
  }

  public void setDBInstanceClass(final String dBInstanceClass) {
    this.dBInstanceClass = dBInstanceClass;
  }

  public Integer getDuration() {
    return duration;
  }

  public void setDuration(final Integer duration) {
    this.duration = duration;
  }

  public Double getFixedPrice() {
    return fixedPrice;
  }

  public void setFixedPrice(final Double fixedPrice) {
    this.fixedPrice = fixedPrice;
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

  public RecurringChargeList getRecurringCharges() {
    return recurringCharges;
  }

  public void setRecurringCharges(final RecurringChargeList recurringCharges) {
    this.recurringCharges = recurringCharges;
  }

  public String getReservedDBInstancesOfferingId() {
    return reservedDBInstancesOfferingId;
  }

  public void setReservedDBInstancesOfferingId(final String reservedDBInstancesOfferingId) {
    this.reservedDBInstancesOfferingId = reservedDBInstancesOfferingId;
  }

  public Double getUsagePrice() {
    return usagePrice;
  }

  public void setUsagePrice(final Double usagePrice) {
    this.usagePrice = usagePrice;
  }

}
