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


public class StackInstance extends EucalyptusData {

  private String account;

  @FieldRegex(FieldRegexValue.ENUM_STACKDRIFTSTATUS)
  private String driftStatus;

  private java.util.Date lastDriftCheckTimestamp;

  private Parameters parameterOverrides;

  private String region;

  private String stackId;

  private String stackSetId;

  @FieldRegex(FieldRegexValue.ENUM_STACKINSTANCESTATUS)
  private String status;

  private String statusReason;

  public String getAccount() {
    return account;
  }

  public void setAccount(final String account) {
    this.account = account;
  }

  public String getDriftStatus() {
    return driftStatus;
  }

  public void setDriftStatus(final String driftStatus) {
    this.driftStatus = driftStatus;
  }

  public java.util.Date getLastDriftCheckTimestamp() {
    return lastDriftCheckTimestamp;
  }

  public void setLastDriftCheckTimestamp(final java.util.Date lastDriftCheckTimestamp) {
    this.lastDriftCheckTimestamp = lastDriftCheckTimestamp;
  }

  public Parameters getParameterOverrides() {
    return parameterOverrides;
  }

  public void setParameterOverrides(final Parameters parameterOverrides) {
    this.parameterOverrides = parameterOverrides;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(final String stackId) {
    this.stackId = stackId;
  }

  public String getStackSetId() {
    return stackSetId;
  }

  public void setStackSetId(final String stackSetId) {
    this.stackSetId = stackSetId;
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
