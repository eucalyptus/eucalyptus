/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2SubnetMappingProperties {

  @Property
  private String allocationId;

  @Property( name = "IPv6Address" )
  private String ipv6Address;

  @Property( name = "PrivateIPv4Address" )
  private String privateIpv4Address;

  @Property
  private String subnetId;

  public String getAllocationId() {
    return allocationId;
  }

  public void setAllocationId(String allocationId) {
    this.allocationId = allocationId;
  }

  public String getIpv6Address() {
    return ipv6Address;
  }

  public void setIpv6Address(String ipv6Address) {
    this.ipv6Address = ipv6Address;
  }

  public String getPrivateIpv4Address() {
    return privateIpv4Address;
  }

  public void setPrivateIpv4Address(String privateIpv4Address) {
    this.privateIpv4Address = privateIpv4Address;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId(String subnetId) {
    this.subnetId = subnetId;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("allocationId", allocationId)
        .add("ipv6Address", ipv6Address)
        .add("privateIpv4Address", privateIpv4Address)
        .add("subnetId", subnetId)
        .toString();
  }
}
