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


public class CreateChangeSetType extends CloudFormationMessage {

  private Capabilities capabilities;

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String changeSetName;

  @FieldRegex(FieldRegexValue.ENUM_CHANGESETTYPE)
  private String changeSetType;

  @FieldRange(min = 1, max = 128)
  private String clientToken;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRange(max = 5)
  private NotificationARNs notificationARNs;

  private Parameters parameters;

  private ResourceTypes resourceTypes;

  @FieldRange(max = 200)
  private ResourcesToImport resourcesToImport;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  private RollbackConfiguration rollbackConfiguration;

  @Nonnull
  @FieldRange(min = 1)
  private String stackName;

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

  public String getChangeSetName() {
    return changeSetName;
  }

  public void setChangeSetName(final String changeSetName) {
    this.changeSetName = changeSetName;
  }

  public String getChangeSetType() {
    return changeSetType;
  }

  public void setChangeSetType(final String changeSetType) {
    this.changeSetType = changeSetType;
  }

  public String getClientToken() {
    return clientToken;
  }

  public void setClientToken(final String clientToken) {
    this.clientToken = clientToken;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
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

  public ResourcesToImport getResourcesToImport() {
    return resourcesToImport;
  }

  public void setResourcesToImport(final ResourcesToImport resourcesToImport) {
    this.resourcesToImport = resourcesToImport;
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
