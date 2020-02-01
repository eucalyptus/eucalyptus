/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeStackDriftDetectionStatusResult extends EucalyptusData {

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STACKDRIFTDETECTIONSTATUS)
  private String detectionStatus;

  private String detectionStatusReason;

  private Integer driftedStackResourceCount;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String stackDriftDetectionId;

  @FieldRegex(FieldRegexValue.ENUM_STACKDRIFTSTATUS)
  private String stackDriftStatus;

  @Nonnull
  private String stackId;

  @Nonnull
  private java.util.Date timestamp;

  public String getDetectionStatus() {
    return detectionStatus;
  }

  public void setDetectionStatus(final String detectionStatus) {
    this.detectionStatus = detectionStatus;
  }

  public String getDetectionStatusReason() {
    return detectionStatusReason;
  }

  public void setDetectionStatusReason(final String detectionStatusReason) {
    this.detectionStatusReason = detectionStatusReason;
  }

  public Integer getDriftedStackResourceCount() {
    return driftedStackResourceCount;
  }

  public void setDriftedStackResourceCount(final Integer driftedStackResourceCount) {
    this.driftedStackResourceCount = driftedStackResourceCount;
  }

  public String getStackDriftDetectionId() {
    return stackDriftDetectionId;
  }

  public void setStackDriftDetectionId(final String stackDriftDetectionId) {
    this.stackDriftDetectionId = stackDriftDetectionId;
  }

  public String getStackDriftStatus() {
    return stackDriftStatus;
  }

  public void setStackDriftStatus(final String stackDriftStatus) {
    this.stackDriftStatus = stackDriftStatus;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(final String stackId) {
    this.stackId = stackId;
  }

  public java.util.Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final java.util.Date timestamp) {
    this.timestamp = timestamp;
  }

}
