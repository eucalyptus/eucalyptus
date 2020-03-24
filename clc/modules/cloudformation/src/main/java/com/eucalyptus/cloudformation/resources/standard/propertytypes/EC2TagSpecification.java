/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class EC2TagSpecification {

  @Property
  private String resourceType;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList();

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

  public ArrayList<EC2Tag> getTags() {
    return tags;
  }

  public void setTags(final ArrayList<EC2Tag> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final EC2TagSpecification that = (EC2TagSpecification) o;
    return Objects.equals(getResourceType(), that.getResourceType()) &&
        Objects.equals(getTags(), that.getTags());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getResourceType(), getTags());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("resourceType", resourceType)
        .add("tags", tags)
        .toString();
  }
}
