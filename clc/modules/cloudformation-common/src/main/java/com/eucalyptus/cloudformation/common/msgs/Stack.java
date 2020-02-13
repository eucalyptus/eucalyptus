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


public class Stack extends EucalyptusData {

  private Capabilities capabilities;

  @FieldRange(min = 1)
  private String changeSetId;

  @Nonnull
  private java.util.Date creationTime;

  private java.util.Date deletionTime;

  @FieldRange(min = 1, max = 1024)
  private String description;

  private Boolean disableRollback;

  private StackDriftInformation driftInformation;

  private Boolean enableTerminationProtection;

  private java.util.Date lastUpdatedTime;

  @FieldRange(max = 5)
  private NotificationARNs notificationARNs;

  private Outputs outputs;

  private Parameters parameters;

  private String parentId;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  private RollbackConfiguration rollbackConfiguration;

  private String rootId;

  private String stackId;

  @Nonnull
  private String stackName;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STACKSTATUS)
  private String stackStatus;

  private String stackStatusReason;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private Integer timeoutInMinutes;

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

  public java.util.Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(final java.util.Date creationTime) {
    this.creationTime = creationTime;
  }

  public java.util.Date getDeletionTime() {
    return deletionTime;
  }

  public void setDeletionTime(final java.util.Date deletionTime) {
    this.deletionTime = deletionTime;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Boolean getDisableRollback() {
    return disableRollback;
  }

  public void setDisableRollback(final Boolean disableRollback) {
    this.disableRollback = disableRollback;
  }

  public StackDriftInformation getDriftInformation() {
    return driftInformation;
  }

  public void setDriftInformation(final StackDriftInformation driftInformation) {
    this.driftInformation = driftInformation;
  }

  public Boolean getEnableTerminationProtection() {
    return enableTerminationProtection;
  }

  public void setEnableTerminationProtection(final Boolean enableTerminationProtection) {
    this.enableTerminationProtection = enableTerminationProtection;
  }

  public java.util.Date getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime(final java.util.Date lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  public NotificationARNs getNotificationARNs() {
    return notificationARNs;
  }

  public void setNotificationARNs(final NotificationARNs notificationARNs) {
    this.notificationARNs = notificationARNs;
  }

  public Outputs getOutputs() {
    return outputs;
  }

  public void setOutputs(final Outputs outputs) {
    this.outputs = outputs;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public void setParameters(final Parameters parameters) {
    this.parameters = parameters;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(final String parentId) {
    this.parentId = parentId;
  }

  public String getRoleARN() {
    return roleARN;
  }

  public void setRoleARN(final String roleARN) {
    this.roleARN = roleARN;
  }

  public RollbackConfiguration getRollbackConfiguration() {
    return rollbackConfiguration;
  }

  public void setRollbackConfiguration(final RollbackConfiguration rollbackConfiguration) {
    this.rollbackConfiguration = rollbackConfiguration;
  }

  public String getRootId() {
    return rootId;
  }

  public void setRootId(final String rootId) {
    this.rootId = rootId;
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

  public String getStackStatus() {
    return stackStatus;
  }

  public void setStackStatus(final String stackStatus) {
    this.stackStatus = stackStatus;
  }

  public String getStackStatusReason() {
    return stackStatusReason;
  }

  public void setStackStatusReason(final String stackStatusReason) {
    this.stackStatusReason = stackStatusReason;
  }

  public Tags getTags() {
    return tags;
  }

  public void setTags(final Tags tags) {
    this.tags = tags;
  }

  public Integer getTimeoutInMinutes() {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes(final Integer timeoutInMinutes) {
    this.timeoutInMinutes = timeoutInMinutes;
  }

}
