/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeOptionGroupsResult extends EucalyptusData {

  private String marker;

  private OptionGroupsList optionGroupsList;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public OptionGroupsList getOptionGroupsList() {
    return optionGroupsList;
  }

  public void setOptionGroupsList(final OptionGroupsList optionGroupsList) {
    this.optionGroupsList = optionGroupsList;
  }

}
