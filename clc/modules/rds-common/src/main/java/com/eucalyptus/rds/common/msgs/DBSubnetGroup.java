/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBSubnetGroup extends EucalyptusData {

  private String dBSubnetGroupArn;

  private String dBSubnetGroupDescription;

  private String dBSubnetGroupName;

  private String subnetGroupStatus;

  private SubnetList subnets;

  private String vpcId;

  public String getDBSubnetGroupArn() {
    return dBSubnetGroupArn;
  }

  public void setDBSubnetGroupArn(final String dBSubnetGroupArn) {
    this.dBSubnetGroupArn = dBSubnetGroupArn;
  }

  public String getDBSubnetGroupDescription() {
    return dBSubnetGroupDescription;
  }

  public void setDBSubnetGroupDescription(final String dBSubnetGroupDescription) {
    this.dBSubnetGroupDescription = dBSubnetGroupDescription;
  }

  public String getDBSubnetGroupName() {
    return dBSubnetGroupName;
  }

  public void setDBSubnetGroupName(final String dBSubnetGroupName) {
    this.dBSubnetGroupName = dBSubnetGroupName;
  }

  public String getSubnetGroupStatus() {
    return subnetGroupStatus;
  }

  public void setSubnetGroupStatus(final String subnetGroupStatus) {
    this.subnetGroupStatus = subnetGroupStatus;
  }

  public SubnetList getSubnets() {
    return subnets;
  }

  public void setSubnets(final SubnetList subnets) {
    this.subnets = subnets;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
