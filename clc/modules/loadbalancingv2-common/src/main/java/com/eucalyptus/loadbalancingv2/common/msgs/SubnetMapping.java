/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SubnetMapping extends EucalyptusData {

  private String allocationId;

  private String privateIPv4Address;

  private String subnetId;

  public String getAllocationId() {
    return allocationId;
  }

  public void setAllocationId(final String allocationId) {
    this.allocationId = allocationId;
  }

  public String getPrivateIPv4Address() {
    return privateIPv4Address;
  }

  public void setPrivateIPv4Address(final String privateIPv4Address) {
    this.privateIPv4Address = privateIPv4Address;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId(final String subnetId) {
    this.subnetId = subnetId;
  }

}
