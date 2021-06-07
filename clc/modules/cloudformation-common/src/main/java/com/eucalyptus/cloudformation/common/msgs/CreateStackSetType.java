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


public class CreateStackSetType extends CloudFormationMessage {

  @FieldRange(min = 20, max = 2048)
  private String administrationRoleARN;

  private Capabilities capabilities;

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRange(min = 1, max = 64)
  private String executionRoleName;

  private Parameters parameters;

  @Nonnull
  private String stackSetName;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private String templateBody;

  @FieldRange(min = 1, max = 1024)
  private String templateURL;

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

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
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

}
