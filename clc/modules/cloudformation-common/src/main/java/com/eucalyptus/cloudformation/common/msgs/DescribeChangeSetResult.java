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


public class DescribeChangeSetResult extends EucalyptusData {

  private Capabilities capabilities;

  @FieldRange(min = 1)
  private String changeSetId;

  @FieldRange(min = 1, max = 128)
  private String changeSetName;

  private Changes changes;

  private java.util.Date creationTime;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRegex(FieldRegexValue.ENUM_EXECUTIONSTATUS)
  private String executionStatus;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  @FieldRange(max = 5)
  private NotificationARNs notificationARNs;

  private Parameters parameters;

  private RollbackConfiguration rollbackConfiguration;

  private String stackId;

  private String stackName;

  @FieldRegex(FieldRegexValue.ENUM_CHANGESETSTATUS)
  private String status;

  private String statusReason;

  @FieldRange(max = 50)
  private Tags tags;

  public Capabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(final Capabilities capabilities) {
    this.capabilities = capabilities;
  }

  public String getChangeSetId() {
    return changeSetId;
  }

  public void setChangeSetId(final String changeSetId) {
    this.changeSetId = changeSetId;
  }

  public String getChangeSetName() {
    return changeSetName;
  }

  public void setChangeSetName(final String changeSetName) {
    this.changeSetName = changeSetName;
  }

  public Changes getChanges() {
    return changes;
  }

  public void setChanges(final Changes changes) {
    this.changes = changes;
  }

  public java.util.Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(final java.util.Date creationTime) {
    this.creationTime = creationTime;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getExecutionStatus() {
    return executionStatus;
  }

  public void setExecutionStatus(final String executionStatus) {
    this.executionStatus = executionStatus;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

  public NotificationARNs getNotificationARNs() {
    return notificationARNs;
  }

  public void setNotificationARNs(final NotificationARNs notificationARNs) {
    this.notificationARNs = notificationARNs;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public void setParameters(final Parameters parameters) {
    this.parameters = parameters;
  }

  public RollbackConfiguration getRollbackConfiguration() {
    return rollbackConfiguration;
  }

  public void setRollbackConfiguration(final RollbackConfiguration rollbackConfiguration) {
    this.rollbackConfiguration = rollbackConfiguration;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(final String stackId) {
    this.stackId = stackId;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
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

  public Tags getTags() {
    return tags;
  }

  public void setTags(final Tags tags) {
    this.tags = tags;
  }

}
