/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBSecurityGroup extends EucalyptusData {

  private String dBSecurityGroupArn;

  private String dBSecurityGroupDescription;

  private String dBSecurityGroupName;

  private EC2SecurityGroupList eC2SecurityGroups;

  private IPRangeList iPRanges;

  private String ownerId;

  private String vpcId;

  public String getDBSecurityGroupArn() {
    return dBSecurityGroupArn;
  }

  public void setDBSecurityGroupArn(final String dBSecurityGroupArn) {
    this.dBSecurityGroupArn = dBSecurityGroupArn;
  }

  public String getDBSecurityGroupDescription() {
    return dBSecurityGroupDescription;
  }

  public void setDBSecurityGroupDescription(final String dBSecurityGroupDescription) {
    this.dBSecurityGroupDescription = dBSecurityGroupDescription;
  }

  public String getDBSecurityGroupName() {
    return dBSecurityGroupName;
  }

  public void setDBSecurityGroupName(final String dBSecurityGroupName) {
    this.dBSecurityGroupName = dBSecurityGroupName;
  }

  public EC2SecurityGroupList getEC2SecurityGroups() {
    return eC2SecurityGroups;
  }

  public void setEC2SecurityGroups(final EC2SecurityGroupList eC2SecurityGroups) {
    this.eC2SecurityGroups = eC2SecurityGroups;
  }

  public IPRangeList getIPRanges() {
    return iPRanges;
  }

  public void setIPRanges(final IPRangeList iPRanges) {
    this.iPRanges = iPRanges;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
