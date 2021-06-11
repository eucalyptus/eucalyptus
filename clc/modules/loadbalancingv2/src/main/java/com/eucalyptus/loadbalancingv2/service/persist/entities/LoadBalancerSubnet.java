/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerSubnetView;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LoadBalancerSubnet implements LoadBalancerSubnetView {

  @Column(name = "metadata_subnet_id", nullable = false, updatable = false)
  private String subnetId;

  @Column(name = "metadata_availability_zone", nullable = false, updatable = false)
  private String availabilityZone;

  public static LoadBalancerSubnet create(
      final String subnetId,
      final String availabilityZone
  ) {
    final LoadBalancerSubnet subnet = new LoadBalancerSubnet();
    subnet.setSubnetId(subnetId);
    subnet.setAvailabilityZone(availabilityZone);
    return subnet;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId(String subnetId) {
    this.subnetId = subnetId;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }
}