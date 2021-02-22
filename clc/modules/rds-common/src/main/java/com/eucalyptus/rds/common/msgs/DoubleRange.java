/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DoubleRange extends EucalyptusData {

  private Double from;

  private Double to;

  public Double getFrom() {
    return from;
  }

  public void setFrom(final Double from) {
    this.from = from;
  }

  public Double getTo() {
    return to;
  }

  public void setTo(final Double to) {
    this.to = to;
  }

}
