/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;


public class AWSRDSDBParameterGroupProperties implements ResourceProperties {

  @Property
  @Required
  private String description;

  @Property
  @Required
  private String family;

  @Property
  private JsonNode parameters;

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getFamily() {
    return family;
  }

  public void setFamily(final String family) {
    this.family = family;
  }

  public JsonNode getParameters() {
    return parameters;
  }

  public void setParameters(final JsonNode parameters) {
    this.parameters = parameters;
  }

  public ArrayList<CloudFormationResourceTag> getTags() {
    return tags;
  }

  public void setTags(final ArrayList<CloudFormationResourceTag> tags) {
    this.tags = tags;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("description", description)
        .add("family", family)
        .add("parameters", parameters)
        .add("tags", tags)
        .toString();
  }
}
