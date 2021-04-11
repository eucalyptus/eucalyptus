/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeEngineDefaultClusterParametersResult extends EucalyptusData {

  private EngineDefaults engineDefaults;

  public EngineDefaults getEngineDefaults() {
    return engineDefaults;
  }

  public void setEngineDefaults(final EngineDefaults engineDefaults) {
    this.engineDefaults = engineDefaults;
  }

}
