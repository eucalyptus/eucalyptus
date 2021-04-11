/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DomainMembership extends EucalyptusData {

  private String domain;

  private String fQDN;

  private String iAMRoleName;

  private String status;

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getFQDN() {
    return fQDN;
  }

  public void setFQDN(final String fQDN) {
    this.fQDN = fQDN;
  }

  public String getIAMRoleName() {
    return iAMRoleName;
  }

  public void setIAMRoleName(final String iAMRoleName) {
    this.iAMRoleName = iAMRoleName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
