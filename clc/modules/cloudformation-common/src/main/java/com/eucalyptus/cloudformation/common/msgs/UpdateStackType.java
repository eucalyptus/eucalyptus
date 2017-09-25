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

public class UpdateStackType extends CloudFormationMessage {

  private ResourceList capabilities;
  private ResourceList notificationARNs;
  private Parameters parameters;
  private ResourceList resourceTypes;
  private String stackName;
  private String stackPolicyBody;
  private String stackPolicyDuringUpdateBody;
  private String stackPolicyDuringUpdateURL;
  private String stackPolicyURL;
  private Tags tags;
  private String templateBody;
  private String templateURL;
  private Boolean usePreviousTemplate;

  public ResourceList getCapabilities( ) {
    return capabilities;
  }

  public void setCapabilities( ResourceList capabilities ) {
    this.capabilities = capabilities;
  }

  public ResourceList getNotificationARNs( ) {
    return notificationARNs;
  }

  public void setNotificationARNs( ResourceList notificationARNs ) {
    this.notificationARNs = notificationARNs;
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

  public String getStackPolicyDuringUpdateBody( ) {
    return stackPolicyDuringUpdateBody;
  }

  public void setStackPolicyDuringUpdateBody( String stackPolicyDuringUpdateBody ) {
    this.stackPolicyDuringUpdateBody = stackPolicyDuringUpdateBody;
  }

  public String getStackPolicyDuringUpdateURL( ) {
    return stackPolicyDuringUpdateURL;
  }

  public void setStackPolicyDuringUpdateURL( String stackPolicyDuringUpdateURL ) {
    this.stackPolicyDuringUpdateURL = stackPolicyDuringUpdateURL;
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

  public Boolean getUsePreviousTemplate( ) {
    return usePreviousTemplate;
  }

  public void setUsePreviousTemplate( Boolean usePreviousTemplate ) {
    this.usePreviousTemplate = usePreviousTemplate;
  }
}
