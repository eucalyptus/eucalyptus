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


public class StackSetDriftDetectionDetails extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_STACKSETDRIFTDETECTIONSTATUS)
  private String driftDetectionStatus;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETDRIFTSTATUS)
  private String driftStatus;

  @FieldRange()
  private Integer driftedStackInstancesCount;

  @FieldRange()
  private Integer failedStackInstancesCount;

  @FieldRange()
  private Integer inProgressStackInstancesCount;

  @FieldRange()
  private Integer inSyncStackInstancesCount;

  private java.util.Date lastDriftCheckTimestamp;

  @FieldRange()
  private Integer totalStackInstancesCount;

  public String getDriftDetectionStatus() {
    return driftDetectionStatus;
  }

  public void setDriftDetectionStatus(final String driftDetectionStatus) {
    this.driftDetectionStatus = driftDetectionStatus;
  }

  public String getDriftStatus() {
    return driftStatus;
  }

  public void setDriftStatus(final String driftStatus) {
    this.driftStatus = driftStatus;
  }

  public Integer getDriftedStackInstancesCount() {
    return driftedStackInstancesCount;
  }

  public void setDriftedStackInstancesCount(final Integer driftedStackInstancesCount) {
    this.driftedStackInstancesCount = driftedStackInstancesCount;
  }

  public Integer getFailedStackInstancesCount() {
    return failedStackInstancesCount;
  }

  public void setFailedStackInstancesCount(final Integer failedStackInstancesCount) {
    this.failedStackInstancesCount = failedStackInstancesCount;
  }

  public Integer getInProgressStackInstancesCount() {
    return inProgressStackInstancesCount;
  }

  public void setInProgressStackInstancesCount(final Integer inProgressStackInstancesCount) {
    this.inProgressStackInstancesCount = inProgressStackInstancesCount;
  }

  public Integer getInSyncStackInstancesCount() {
    return inSyncStackInstancesCount;
  }

  public void setInSyncStackInstancesCount(final Integer inSyncStackInstancesCount) {
    this.inSyncStackInstancesCount = inSyncStackInstancesCount;
  }

  public java.util.Date getLastDriftCheckTimestamp() {
    return lastDriftCheckTimestamp;
  }

  public void setLastDriftCheckTimestamp(final java.util.Date lastDriftCheckTimestamp) {
    this.lastDriftCheckTimestamp = lastDriftCheckTimestamp;
  }

  public Integer getTotalStackInstancesCount() {
    return totalStackInstancesCount;
  }

  public void setTotalStackInstancesCount(final Integer totalStackInstancesCount) {
    this.totalStackInstancesCount = totalStackInstancesCount;
  }

}
