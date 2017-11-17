/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSCloudFormationStackProperties implements ResourceProperties {

  @Property
  private ArrayList<String> notificationARNs = Lists.newArrayList( );

  @Property
  private JsonNode parameters;

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  @Required
  @Property
  private String templateURL;

  @Property
  private Integer timeoutInMinutes;

  public ArrayList<String> getNotificationARNs( ) {
    return notificationARNs;
  }

  public void setNotificationARNs( ArrayList<String> notificationARNs ) {
    this.notificationARNs = notificationARNs;
  }

  public JsonNode getParameters( ) {
    return parameters;
  }

  public void setParameters( JsonNode parameters ) {
    this.parameters = parameters;
  }

  public ArrayList<CloudFormationResourceTag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<CloudFormationResourceTag> tags ) {
    this.tags = tags;
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

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "notificationARNs", notificationARNs )
        .add( "parameters", parameters )
        .add( "tags", tags )
        .add( "templateURL", templateURL )
        .add( "timeoutInMinutes", timeoutInMinutes )
        .toString( );
  }
}
