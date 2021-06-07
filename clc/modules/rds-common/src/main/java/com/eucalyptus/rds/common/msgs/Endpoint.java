/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Endpoint extends EucalyptusData {

  private String address;

  private String hostedZoneId;

  private Integer port;

  public String getAddress() {
    return address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

}
