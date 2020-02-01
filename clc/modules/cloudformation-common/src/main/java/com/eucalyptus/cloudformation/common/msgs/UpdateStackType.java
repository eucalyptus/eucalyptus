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


public class UpdateStackType extends CloudFormationMessage {

  private Capabilities capabilities;

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @FieldRange(max = 5)
  private NotificationARNs notificationARNs;

  private Parameters parameters;

  private ResourceTypes resourceTypes;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  private RollbackConfiguration rollbackConfiguration;

  @Nonnull
  private String stackName;

  @FieldRange(min = 1, max = 16384)
  private String stackPolicyBody;

  @FieldRange(min = 1, max = 16384)
  private String stackPolicyDuringUpdateBody;

  @FieldRange(min = 1, max = 1350)
  private String stackPolicyDuringUpdateURL;

  @FieldRange(min = 1, max = 1350)
  private String stackPolicyURL;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private String templateBody;

  @FieldRange(min = 1, max = 1024)
  private String templateURL;

  private Boolean usePreviousTemplate;

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

  public ResourceTypes getResourceTypes() {
    return resourceTypes;
  }

  public void setResourceTypes(final ResourceTypes resourceTypes) {
    this.resourceTypes = resourceTypes;
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

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public String getStackPolicyBody() {
    return stackPolicyBody;
  }

  public void setStackPolicyBody(final String stackPolicyBody) {
    this.stackPolicyBody = stackPolicyBody;
  }

  public String getStackPolicyDuringUpdateBody() {
    return stackPolicyDuringUpdateBody;
  }

  public void setStackPolicyDuringUpdateBody(final String stackPolicyDuringUpdateBody) {
    this.stackPolicyDuringUpdateBody = stackPolicyDuringUpdateBody;
  }

  public String getStackPolicyDuringUpdateURL() {
    return stackPolicyDuringUpdateURL;
  }

  public void setStackPolicyDuringUpdateURL(final String stackPolicyDuringUpdateURL) {
    this.stackPolicyDuringUpdateURL = stackPolicyDuringUpdateURL;
  }

  public String getStackPolicyURL() {
    return stackPolicyURL;
  }

  public void setStackPolicyURL(final String stackPolicyURL) {
    this.stackPolicyURL = stackPolicyURL;
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
