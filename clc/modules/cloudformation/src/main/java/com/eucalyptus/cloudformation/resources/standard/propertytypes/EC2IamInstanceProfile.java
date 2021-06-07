/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class EC2IamInstanceProfile {

  @Property
  private String arn;

  @Property
  private String name;

  public String getArn() {
    return arn;
  }

  public void setArn(final String arn) {
    this.arn = arn;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final EC2IamInstanceProfile that = (EC2IamInstanceProfile) o;
    return Objects.equals(getArn(), that.getArn()) &&
        Objects.equals(getName(), that.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getArn(), getName());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("arn", arn)
        .add("name", name)
        .toString();
  }
}
