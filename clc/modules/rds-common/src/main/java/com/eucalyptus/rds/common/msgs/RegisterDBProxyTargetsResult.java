/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RegisterDBProxyTargetsResult extends EucalyptusData {

  private TargetList dBProxyTargets;

  public TargetList getDBProxyTargets() {
    return dBProxyTargets;
  }

  public void setDBProxyTargets(final TargetList dBProxyTargets) {
    this.dBProxyTargets = dBProxyTargets;
  }

}
