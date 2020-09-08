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
public class EC2Monitoring {

  @Property
  private Boolean enabled;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final EC2Monitoring that = (EC2Monitoring) o;
    return Objects.equals(getEnabled(), that.getEnabled());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEnabled());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("enabled", enabled)
        .toString();
  }
}
