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


public class CreateStackType extends CloudFormationMessage {

  private Capabilities capabilities;

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  private Boolean disableRollback;

  private Boolean enableTerminationProtection;

  @FieldRange(max = 5)
  private NotificationARNs notificationARNs;

  @FieldRegex(FieldRegexValue.ENUM_ONFAILURE)
  private String onFailure;

  private Parameters parameters;

  private ResourceTypes resourceTypes;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  private RollbackConfiguration rollbackConfiguration;

  @Nonnull
  private String stackName;

  @FieldRange(min = 1, max = 16384)
  private String stackPolicyBody;

  @FieldRange(min = 1, max = 1350)
  private String stackPolicyURL;

  @FieldRange(max = 50)
  private Tags tags;

  @FieldRange(min = 1)
  private String templateBody;

  @FieldRange(min = 1, max = 1024)
  private String templateURL;

  @FieldRange(min = 1)
  private Integer timeoutInMinutes;

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

  public Boolean getDisableRollback() {
    return disableRollback;
  }

  public void setDisableRollback(final Boolean disableRollback) {
    this.disableRollback = disableRollback;
  }

  public Boolean getEnableTerminationProtection() {
    return enableTerminationProtection;
  }

  public void setEnableTerminationProtection(final Boolean enableTerminationProtection) {
    this.enableTerminationProtection = enableTerminationProtection;
  }

  public NotificationARNs getNotificationARNs() {
    return notificationARNs;
  }

  public void setNotificationARNs(final NotificationARNs notificationARNs) {
    this.notificationARNs = notificationARNs;
  }

  public String getOnFailure() {
    return onFailure;
  }

  public void setOnFailure(final String onFailure) {
    this.onFailure = onFailure;
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

  public Integer getTimeoutInMinutes() {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes(final Integer timeoutInMinutes) {
    this.timeoutInMinutes = timeoutInMinutes;
  }

}
