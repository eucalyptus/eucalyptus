/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Range extends EucalyptusData {

  private Integer from;

  private Integer step;

  private Integer to;

  public Integer getFrom() {
    return from;
  }

  public void setFrom(final Integer from) {
    this.from = from;
  }

  public Integer getStep() {
    return step;
  }

  public void setStep(final Integer step) {
    this.step = step;
  }

  public Integer getTo() {
    return to;
  }

  public void setTo(final Integer to) {
    this.to = to;
  }

}
