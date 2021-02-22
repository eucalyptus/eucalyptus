/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class MinimumEngineVersionPerAllowedValue extends EucalyptusData {

  private String allowedValue;

  private String minimumEngineVersion;

  public String getAllowedValue() {
    return allowedValue;
  }

  public void setAllowedValue(final String allowedValue) {
    this.allowedValue = allowedValue;
  }

  public String getMinimumEngineVersion() {
    return minimumEngineVersion;
  }

  public void setMinimumEngineVersion(final String minimumEngineVersion) {
    this.minimumEngineVersion = minimumEngineVersion;
  }

}
