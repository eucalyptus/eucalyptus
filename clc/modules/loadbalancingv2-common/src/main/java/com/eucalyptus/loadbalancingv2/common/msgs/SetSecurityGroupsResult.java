/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SetSecurityGroupsResult extends EucalyptusData {

  private SecurityGroups securityGroupIds;

  public SecurityGroups getSecurityGroupIds() {
    return securityGroupIds;
  }

  public void setSecurityGroupIds(final SecurityGroups securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
  }

}
