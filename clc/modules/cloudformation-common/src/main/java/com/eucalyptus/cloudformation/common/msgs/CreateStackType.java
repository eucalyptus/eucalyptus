/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cloudformation.common.msgs;

public class CreateStackType extends CloudFormationMessage {

  private ResourceList capabilities;
  private Boolean disableRollback;
  private ResourceList notificationARNs;
  private String onFailure;
  private Parameters parameters;
  private ResourceList resourceTypes;
  private String stackName;
  private String stackPolicyBody;
  private String stackPolicyURL;
  private Tags tags;
  private String templateBody;
  private String templateURL;
  private Integer timeoutInMinutes;

  public ResourceList getCapabilities( ) {
    return capabilities;
  }

  public void setCapabilities( ResourceList capabilities ) {
    this.capabilities = capabilities;
  }

  public Boolean getDisableRollback( ) {
    return disableRollback;
  }

  public void setDisableRollback( Boolean disableRollback ) {
    this.disableRollback = disableRollback;
  }

  public ResourceList getNotificationARNs( ) {
    return notificationARNs;
  }

  public void setNotificationARNs( ResourceList notificationARNs ) {
    this.notificationARNs = notificationARNs;
  }

  public String getOnFailure( ) {
    return onFailure;
  }

  public void setOnFailure( String onFailure ) {
    this.onFailure = onFailure;
  }

  public Parameters getParameters( ) {
    return parameters;
  }

  public void setParameters( Parameters parameters ) {
    this.parameters = parameters;
  }

  public ResourceList getResourceTypes( ) {
    return resourceTypes;
  }

  public void setResourceTypes( ResourceList resourceTypes ) {
    this.resourceTypes = resourceTypes;
  }

  public String getStackName( ) {
    return stackName;
  }

  public void setStackName( String stackName ) {
    this.stackName = stackName;
  }

  public String getStackPolicyBody( ) {
    return stackPolicyBody;
  }

  public void setStackPolicyBody( String stackPolicyBody ) {
    this.stackPolicyBody = stackPolicyBody;
  }

  public String getStackPolicyURL( ) {
    return stackPolicyURL;
  }

  public void setStackPolicyURL( String stackPolicyURL ) {
    this.stackPolicyURL = stackPolicyURL;
  }

  public Tags getTags( ) {
    return tags;
  }

  public void setTags( Tags tags ) {
    this.tags = tags;
  }

  public String getTemplateBody( ) {
    return templateBody;
  }

  public void setTemplateBody( String templateBody ) {
    this.templateBody = templateBody;
  }

  public String getTemplateURL( ) {
    return templateURL;
  }

  public void setTemplateURL( String templateURL ) {
    this.templateURL = templateURL;
  }

  public Integer getTimeoutInMinutes( ) {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes( Integer timeoutInMinutes ) {
    this.timeoutInMinutes = timeoutInMinutes;
  }
}
