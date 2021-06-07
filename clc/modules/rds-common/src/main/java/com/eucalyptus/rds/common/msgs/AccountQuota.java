/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AccountQuota extends EucalyptusData {

  private String accountQuotaName;

  private Long max;

  private Long used;

  public String getAccountQuotaName() {
    return accountQuotaName;
  }

  public void setAccountQuotaName(final String accountQuotaName) {
    this.accountQuotaName = accountQuotaName;
  }

  public Long getMax() {
    return max;
  }

  public void setMax(final Long max) {
    this.max = max;
  }

  public Long getUsed() {
    return used;
  }

  public void setUsed(final Long used) {
    this.used = used;
  }

}
