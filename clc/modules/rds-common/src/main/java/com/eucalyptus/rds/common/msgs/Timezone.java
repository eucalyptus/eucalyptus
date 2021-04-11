/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Timezone extends EucalyptusData {

  private String timezoneName;

  public String getTimezoneName() {
    return timezoneName;
  }

  public void setTimezoneName(final String timezoneName) {
    this.timezoneName = timezoneName;
  }

}
