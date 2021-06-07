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


public class StackSetOperation extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_STACKSETOPERATIONACTION)
  private String action;

  @FieldRange(min = 20, max = 2048)
  private String administrationRoleARN;

  private java.util.Date creationTimestamp;

  private java.util.Date endTimestamp;

  @FieldRange(min = 1, max = 64)
  private String executionRoleName;

  @FieldRange(min = 1, max = 128)
  private String operationId;

  private StackSetOperationPreferences operationPreferences;

  private Boolean retainStacks;

  private StackSetDriftDetectionDetails stackSetDriftDetectionDetails;

  private String stackSetId;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETOPERATIONSTATUS)
  private String status;

  public String getAction() {
    return action;
  }

  public void setAction(final String action) {
    this.action = action;
  }

  public String getAdministrationRoleARN() {
    return administrationRoleARN;
  }

  public void setAdministrationRoleARN(final String administrationRoleARN) {
    this.administrationRoleARN = administrationRoleARN;
  }

  public java.util.Date getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(final java.util.Date creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public java.util.Date getEndTimestamp() {
    return endTimestamp;
  }

  public void setEndTimestamp(final java.util.Date endTimestamp) {
    this.endTimestamp = endTimestamp;
  }

  public String getExecutionRoleName() {
    return executionRoleName;
  }

  public void setExecutionRoleName(final String executionRoleName) {
    this.executionRoleName = executionRoleName;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(final String operationId) {
    this.operationId = operationId;
  }

  public StackSetOperationPreferences getOperationPreferences() {
    return operationPreferences;
  }

  public void setOperationPreferences(final StackSetOperationPreferences operationPreferences) {
    this.operationPreferences = operationPreferences;
  }

  public Boolean getRetainStacks() {
    return retainStacks;
  }

  public void setRetainStacks(final Boolean retainStacks) {
    this.retainStacks = retainStacks;
  }

  public StackSetDriftDetectionDetails getStackSetDriftDetectionDetails() {
    return stackSetDriftDetectionDetails;
  }

  public void setStackSetDriftDetectionDetails(final StackSetDriftDetectionDetails stackSetDriftDetectionDetails) {
    this.stackSetDriftDetectionDetails = stackSetDriftDetectionDetails;
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

}
