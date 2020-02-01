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


public class UpdateStackSetType extends CloudFormationMessage {

  private AccountList accounts;

  @FieldRange(min = 20, max = 2048)
  private String administrationRoleARN;

  private Capabilities capabilities;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRange(min = 1, max = 64)
  private String executionRoleName;

  @FieldRange(min = 1, max = 128)
  private String operationId;

  private StackSetOperationPreferences operationPreferences;

  private Parameters parameters;

  private RegionList regions;

  @Nonnull
  private String stackSetName;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private String templateBody;

  @FieldRange(min = 1, max = 1024)
  private String templateURL;

  private Boolean usePreviousTemplate;

  public AccountList getAccounts() {
    return accounts;
  }

  public void setAccounts(final AccountList accounts) {
    this.accounts = accounts;
  }

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

  public Parameters getParameters() {
    return parameters;
  }

  public void setParameters(final Parameters parameters) {
    this.parameters = parameters;
  }

  public RegionList getRegions() {
    return regions;
  }

  public void setRegions(final RegionList regions) {
    this.regions = regions;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
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

  public String getTemplateURL() {
    return templateURL;
  }

  public void setTemplateURL(final String templateURL) {
    this.templateURL = templateURL;
  }

  public Boolean getUsePreviousTemplate() {
    return usePreviousTemplate;
  }

  public void setUsePreviousTemplate(final Boolean usePreviousTemplate) {
    this.usePreviousTemplate = usePreviousTemplate;
  }

}
