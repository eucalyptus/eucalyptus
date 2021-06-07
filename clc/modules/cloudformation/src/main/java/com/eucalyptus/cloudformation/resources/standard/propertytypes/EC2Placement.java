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
public class EC2Placement {

  @Property
  private String affinity;

  @Property
  private String availabilityZone;

  @Property
  private String groupName;

  @Property
  private String hostId;

  @Property
  private String tenancy;

  public String getAffinity() {
    return affinity;
  }

  public void setAffinity(final String affinity) {
    this.affinity = affinity;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(final String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(final String groupName) {
    this.groupName = groupName;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(final String hostId) {
    this.hostId = hostId;
  }

  public String getTenancy() {
    return tenancy;
  }

  public void setTenancy(final String tenancy) {
    this.tenancy = tenancy;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final EC2Placement that = (EC2Placement) o;
    return Objects.equals(getAffinity(), that.getAffinity()) &&
        Objects.equals(getAvailabilityZone(), that.getAvailabilityZone()) &&
        Objects.equals(getGroupName(), that.getGroupName()) &&
        Objects.equals(getHostId(), that.getHostId()) &&
        Objects.equals(getTenancy(), that.getTenancy());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getAffinity(),
        getAvailabilityZone(),
        getGroupName(),
        getHostId(),
        getTenancy());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("affinity", affinity)
        .add("availabilityZone", availabilityZone)
        .add("groupName", groupName)
        .add("hostId", hostId)
        .add("tenancy", tenancy)
        .toString();
  }
}
