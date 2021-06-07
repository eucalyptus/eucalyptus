/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class Route53HostedZoneConfig {

  @Property
  @Required
  private String comment;

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Route53HostedZoneConfig that = (Route53HostedZoneConfig) o;
    return Objects.equals(getComment(), that.getComment());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getComment());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("comment", comment)
        .toString();
  }
}
