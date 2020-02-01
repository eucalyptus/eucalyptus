/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackSummary extends EucalyptusData {

  @Nonnull
  private java.util.Date creationTime;

  private java.util.Date deletionTime;

  private StackDriftInformationSummary driftInformation;

  private java.util.Date lastUpdatedTime;

  private String parentId;

  private String rootId;

  private String stackId;

  @Nonnull
  private String stackName;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STACKSTATUS)
  private String stackStatus;

  private String stackStatusReason;

  private String templateDescription;

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

  public StackDriftInformationSummary getDriftInformation() {
    return driftInformation;
  }

  public void setDriftInformation(final StackDriftInformationSummary driftInformation) {
    this.driftInformation = driftInformation;
  }

  public java.util.Date getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime(final java.util.Date lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(final String parentId) {
    this.parentId = parentId;
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

  public String getTemplateDescription() {
    return templateDescription;
  }

  public void setTemplateDescription(final String templateDescription) {
    this.templateDescription = templateDescription;
  }

}
