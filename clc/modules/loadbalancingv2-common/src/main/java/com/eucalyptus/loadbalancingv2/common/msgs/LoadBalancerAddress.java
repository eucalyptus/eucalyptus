/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadBalancerAddress extends EucalyptusData {

  private String allocationId;

  private String ipAddress;

  private String privateIPv4Address;

  public String getAllocationId() {
    return allocationId;
  }

  public void setAllocationId(final String allocationId) {
    this.allocationId = allocationId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(final String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getPrivateIPv4Address() {
    return privateIPv4Address;
  }

  public void setPrivateIPv4Address(final String privateIPv4Address) {
    this.privateIPv4Address = privateIPv4Address;
  }

}
