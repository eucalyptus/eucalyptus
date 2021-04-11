/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateGlobalClusterResult extends EucalyptusData {

  private GlobalCluster globalCluster;

  public GlobalCluster getGlobalCluster() {
    return globalCluster;
  }

  public void setGlobalCluster(final GlobalCluster globalCluster) {
    this.globalCluster = globalCluster;
  }

}
