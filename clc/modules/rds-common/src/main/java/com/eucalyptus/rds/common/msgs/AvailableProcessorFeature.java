/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AvailableProcessorFeature extends EucalyptusData {

  private String allowedValues;

  private String defaultValue;

  private String name;

  public String getAllowedValues() {
    return allowedValues;
  }

  public void setAllowedValues(final String allowedValues) {
    this.allowedValues = allowedValues;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(final String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

}
