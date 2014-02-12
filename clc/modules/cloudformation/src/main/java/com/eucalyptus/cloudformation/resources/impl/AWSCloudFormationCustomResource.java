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
package com.eucalyptus.cloudformation.resources.impl;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver;
import com.eucalyptus.cloudformation.resources.propertytypes.AWSCloudFormationCustomResourceAttributes;
import com.eucalyptus.cloudformation.resources.propertytypes.AWSCloudFormationCustomResourceProperties;
import com.eucalyptus.cloudformation.resources.propertytypes.ResourceAttributes;
import com.eucalyptus.cloudformation.resources.propertytypes.ResourceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import groovy.transform.ToString;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationCustomResource extends Resource {

  private AWSCloudFormationCustomResourceProperties resourceProperties = new AWSCloudFormationCustomResourceProperties();
  private AWSCloudFormationCustomResourceAttributes resourceAttributes = new AWSCloudFormationCustomResourceAttributes();


  public AWSCloudFormationCustomResource() {
    setType("AWS::CloudFormation::CustomResource");
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return resourceProperties;
  }

  @Override
  public ResourceAttributes getResourceAttributes() {
    return resourceAttributes;
  }

  @Override
  public void populateResourceProperties(JsonNode jsonNode) throws CloudFormationException {
    ResourcePropertyResolver.populateResourceProperties(resourceProperties, jsonNode);
  }

  @Override
  public void create() throws Exception {
    throw new UnsupportedOperationException();
  }


  @Override
  public void delete() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rollback() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public JsonNode referenceValue() {
    return null;
  }
}
