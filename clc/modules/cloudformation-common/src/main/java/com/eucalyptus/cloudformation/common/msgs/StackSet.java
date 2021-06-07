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


public class StackSet extends EucalyptusData {

  @FieldRange(min = 20, max = 2048)
  private String administrationRoleARN;

  private Capabilities capabilities;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRange(min = 1, max = 64)
  private String executionRoleName;

  private Parameters parameters;

  private String stackSetARN;

  private StackSetDriftDetectionDetails stackSetDriftDetectionDetails;

  private String stackSetId;

  private String stackSetName;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETSTATUS)
  private String status;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private String templateBody;

  public String getAdministrationRoleARN() {
    return administrationRoleARN;
  }

  public void setAdministrationRoleARN(final String administrationRoleARN) {
    this.administrationRoleARN = administrationRoleARN;
  }

  public Capabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(final Capabilities capabilities) {
    this.capabilities = capabilities;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getExecutionRoleName() {
    return executionRoleName;
  }

  public void setExecutionRoleName(final String executionRoleName) {
    this.executionRoleName = executionRoleName;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public void setParameters(final Parameters parameters) {
    this.parameters = parameters;
  }

  public String getStackSetARN() {
    return stackSetARN;
  }

  public void setStackSetARN(final String stackSetARN) {
    this.stackSetARN = stackSetARN;
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

  public Tags getTags() {
    return tags;
  }

  public void setTags(final Tags tags) {
    this.tags = tags;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public void setTemplateBody(final String templateBody) {
    this.templateBody = templateBody;
  }

}
