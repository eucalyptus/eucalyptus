/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class VpcSecurityGroupMembership extends EucalyptusData {

  private String status;

  private String vpcSecurityGroupId;

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getVpcSecurityGroupId() {
    return vpcSecurityGroupId;
  }

  public void setVpcSecurityGroupId(final String vpcSecurityGroupId) {
    this.vpcSecurityGroupId = vpcSecurityGroupId;
  }

}
