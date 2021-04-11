/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeAccountAttributesResult extends EucalyptusData {

  private AccountQuotaList accountQuotas;

  public AccountQuotaList getAccountQuotas() {
    return accountQuotas;
  }

  public void setAccountQuotas(final AccountQuotaList accountQuotas) {
    this.accountQuotas = accountQuotas;
  }

}
