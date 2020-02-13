/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackSetSummary extends EucalyptusData {

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRegex(FieldRegexValue.ENUM_STACKDRIFTSTATUS)
  private String driftStatus;

  private java.util.Date lastDriftCheckTimestamp;

  private String stackSetId;

  private String stackSetName;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETSTATUS)
  private String status;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
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

  public String getStackSetId() {
    return stackSetId;
  }

  public void setStackSetId(final String stackSetId) {
    this.stackSetId = stackSetId;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
