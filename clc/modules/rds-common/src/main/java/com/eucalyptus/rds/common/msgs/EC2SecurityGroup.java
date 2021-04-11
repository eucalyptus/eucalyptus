/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class EC2SecurityGroup extends EucalyptusData {

  private String eC2SecurityGroupId;

  private String eC2SecurityGroupName;

  private String eC2SecurityGroupOwnerId;

  private String status;

  public String getEC2SecurityGroupId() {
    return eC2SecurityGroupId;
  }

  public void setEC2SecurityGroupId(final String eC2SecurityGroupId) {
    this.eC2SecurityGroupId = eC2SecurityGroupId;
  }

  public String getEC2SecurityGroupName() {
    return eC2SecurityGroupName;
  }

  public void setEC2SecurityGroupName(final String eC2SecurityGroupName) {
    this.eC2SecurityGroupName = eC2SecurityGroupName;
  }

  public String getEC2SecurityGroupOwnerId() {
    return eC2SecurityGroupOwnerId;
  }

  public void setEC2SecurityGroupOwnerId(final String eC2SecurityGroupOwnerId) {
    this.eC2SecurityGroupOwnerId = eC2SecurityGroupOwnerId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
