/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ReservedDBInstance extends EucalyptusData {

  private String currencyCode;

  private String dBInstanceClass;

  private Integer dBInstanceCount;

  private Integer duration;

  private Double fixedPrice;

  private String leaseId;

  private Boolean multiAZ;

  private String offeringType;

  private String productDescription;

  private RecurringChargeList recurringCharges;

  private String reservedDBInstanceArn;

  private String reservedDBInstanceId;

  private String reservedDBInstancesOfferingId;

  private java.util.Date startTime;

  private String state;

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

  public Integer getDBInstanceCount() {
    return dBInstanceCount;
  }

  public void setDBInstanceCount(final Integer dBInstanceCount) {
    this.dBInstanceCount = dBInstanceCount;
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

  public String getLeaseId() {
    return leaseId;
  }

  public void setLeaseId(final String leaseId) {
    this.leaseId = leaseId;
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

  public String getReservedDBInstanceArn() {
    return reservedDBInstanceArn;
  }

  public void setReservedDBInstanceArn(final String reservedDBInstanceArn) {
    this.reservedDBInstanceArn = reservedDBInstanceArn;
  }

  public String getReservedDBInstanceId() {
    return reservedDBInstanceId;
  }

  public void setReservedDBInstanceId(final String reservedDBInstanceId) {
    this.reservedDBInstanceId = reservedDBInstanceId;
  }

  public String getReservedDBInstancesOfferingId() {
    return reservedDBInstancesOfferingId;
  }

  public void setReservedDBInstancesOfferingId(final String reservedDBInstancesOfferingId) {
    this.reservedDBInstancesOfferingId = reservedDBInstancesOfferingId;
  }

  public java.util.Date getStartTime() {
    return startTime;
  }

  public void setStartTime(final java.util.Date startTime) {
    this.startTime = startTime;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
  }

  public Double getUsagePrice() {
    return usagePrice;
  }

  public void setUsagePrice(final Double usagePrice) {
    this.usagePrice = usagePrice;
  }

}
