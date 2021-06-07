/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackSetOperationResultSummary extends EucalyptusData {

  private String account;

  private AccountGateResult accountGateResult;

  private String region;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETOPERATIONRESULTSTATUS)
  private String status;

  private String statusReason;

  public String getAccount() {
    return account;
  }

  public void setAccount(final String account) {
    this.account = account;
  }

  public AccountGateResult getAccountGateResult() {
    return accountGateResult;
  }

  public void setAccountGateResult(final AccountGateResult accountGateResult) {
    this.accountGateResult = accountGateResult;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public void setStatusReason(final String statusReason) {
    this.statusReason = statusReason;
  }

}
