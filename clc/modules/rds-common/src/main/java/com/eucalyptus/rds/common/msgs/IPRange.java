/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class IPRange extends EucalyptusData {

  private String cIDRIP;

  private String status;

  public String getCIDRIP() {
    return cIDRIP;
  }

  public void setCIDRIP(final String cIDRIP) {
    this.cIDRIP = cIDRIP;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
