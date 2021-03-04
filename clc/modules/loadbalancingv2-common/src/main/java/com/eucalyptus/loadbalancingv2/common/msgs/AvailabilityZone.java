/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AvailabilityZone extends EucalyptusData {

  private LoadBalancerAddresses loadBalancerAddresses;

  private String subnetId;

  private String zoneName;

  public LoadBalancerAddresses getLoadBalancerAddresses() {
    return loadBalancerAddresses;
  }

  public void setLoadBalancerAddresses(final LoadBalancerAddresses loadBalancerAddresses) {
    this.loadBalancerAddresses = loadBalancerAddresses;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId(final String subnetId) {
    this.subnetId = subnetId;
  }

  public String getZoneName() {
    return zoneName;
  }

  public void setZoneName(final String zoneName) {
    this.zoneName = zoneName;
  }

}
