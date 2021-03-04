/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateTargetGroupResult extends EucalyptusData {

  private TargetGroups targetGroups;

  public TargetGroups getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(final TargetGroups targetGroups) {
    this.targetGroups = targetGroups;
  }

}
