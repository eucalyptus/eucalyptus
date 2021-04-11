/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RecurringCharge extends EucalyptusData {

  private Double recurringChargeAmount;

  private String recurringChargeFrequency;

  public Double getRecurringChargeAmount() {
    return recurringChargeAmount;
  }

  public void setRecurringChargeAmount(final Double recurringChargeAmount) {
    this.recurringChargeAmount = recurringChargeAmount;
  }

  public String getRecurringChargeFrequency() {
    return recurringChargeFrequency;
  }

  public void setRecurringChargeFrequency(final String recurringChargeFrequency) {
    this.recurringChargeFrequency = recurringChargeFrequency;
  }

}
