/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources

import com.eucalyptus.cloudformation.CloudFormationException
import com.google.common.collect.Lists

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class ResourceInfo {
  Boolean ready = false;
  String propertiesJson;
  String metadataJson;
  String updatePolicyJson;
  String deletionPolicy = "Delete";
  public boolean supportsSnapshot() {
    return false;
  }
  public boolean canCheckAttributes() {
    return true;
  }
  public Collection<String> getRequiredCapabilities() {
    return new ArrayList<String>();
  }
  String accountId;
  String effectiveUserId;
  String type;
  String logicalResourceId;
  String physicalResourceId;
  Boolean allowedByCondition;
  String referenceValueJson;
  String description;

  public String getResourceAttributeJson(String attributeName)
    throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeJson(this, attributeName);
  }

  public void setResourceAttributeJson(String attributeName, String attributeValueJson)
    throws CloudFormationException {
    ResourceAttributeResolver.setResourceAttributeJson(this, attributeName, attributeValueJson);
  }

  public Collection<String> getAttributeNames() throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeNames(this);
  }

}
