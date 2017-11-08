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
