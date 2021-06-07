/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeOptionGroupOptionsResult extends EucalyptusData {

  private String marker;

  private OptionGroupOptionsList optionGroupOptions;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public OptionGroupOptionsList getOptionGroupOptions() {
    return optionGroupOptions;
  }

  public void setOptionGroupOptions(final OptionGroupOptionsList optionGroupOptions) {
    this.optionGroupOptions = optionGroupOptions;
  }

}
