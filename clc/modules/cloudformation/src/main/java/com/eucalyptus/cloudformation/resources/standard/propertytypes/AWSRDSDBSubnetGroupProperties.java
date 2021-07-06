/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class AWSRDSDBSubnetGroupProperties implements ResourceProperties {

  @Required
  @Property(name="DBSubnetGroupDescription")
  private String dbSubnetGroupDescription;

  @Property(name="DBSubnetGroupName")
  private String dbSubnetGroupName;

  @Required
  @Property
  private ArrayList<String> subnetIds = Lists.newArrayList( );

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  public String getDbSubnetGroupDescription() {
    return dbSubnetGroupDescription;
  }

  public void setDbSubnetGroupDescription(final String dbSubnetGroupDescription) {
    this.dbSubnetGroupDescription = dbSubnetGroupDescription;
  }

  public String getDbSubnetGroupName() {
    return dbSubnetGroupName;
  }

  public void setDbSubnetGroupName(final String dbSubnetGroupName) {
    this.dbSubnetGroupName = dbSubnetGroupName;
  }

  public ArrayList<String> getSubnetIds() {
    return subnetIds;
  }

  public void setSubnetIds(final ArrayList<String> subnetIds) {
    this.subnetIds = subnetIds;
  }

  public ArrayList<CloudFormationResourceTag> getTags() {
    return tags;
  }

  public void setTags(final ArrayList<CloudFormationResourceTag> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dbSubnetGroupDescription", dbSubnetGroupDescription)
        .add("dbSubnetGroupName", dbSubnetGroupName)
        .add("subnetIds", subnetIds)
        .add("tags", tags)
        .toString();
  }
}
