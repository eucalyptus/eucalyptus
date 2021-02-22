/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RevokeDBSecurityGroupIngressResult extends EucalyptusData {

  private DBSecurityGroup dBSecurityGroup;

  public DBSecurityGroup getDBSecurityGroup() {
    return dBSecurityGroup;
  }

  public void setDBSecurityGroup(final DBSecurityGroup dBSecurityGroup) {
    this.dBSecurityGroup = dBSecurityGroup;
  }

}
