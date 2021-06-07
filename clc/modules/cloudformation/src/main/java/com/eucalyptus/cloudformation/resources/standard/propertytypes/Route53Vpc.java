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
public class Route53Vpc {

  @Property(name = "VPCId")
  @Required
  private String vpcId;

  @Property(name = "VPCRegion")
  @Required
  private String vpcRegion;

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

  public String getVpcRegion() {
    return vpcRegion;
  }

  public void setVpcRegion(final String vpcRegion) {
    this.vpcRegion = vpcRegion;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Route53Vpc that = (Route53Vpc) o;
    return Objects.equals(getVpcId(), that.getVpcId()) &&
        Objects.equals(getVpcRegion(), that.getVpcRegion());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getVpcId(), getVpcRegion());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("vpcId", vpcId)
        .add("vpcRegion", vpcRegion)
        .toString();
  }
}
