/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ScalingConfiguration extends EucalyptusData {

  private Boolean autoPause;

  private Integer maxCapacity;

  private Integer minCapacity;

  private Integer secondsUntilAutoPause;

  private String timeoutAction;

  public Boolean getAutoPause() {
    return autoPause;
  }

  public void setAutoPause(final Boolean autoPause) {
    this.autoPause = autoPause;
  }

  public Integer getMaxCapacity() {
    return maxCapacity;
  }

  public void setMaxCapacity(final Integer maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  public Integer getMinCapacity() {
    return minCapacity;
  }

  public void setMinCapacity(final Integer minCapacity) {
    this.minCapacity = minCapacity;
  }

  public Integer getSecondsUntilAutoPause() {
    return secondsUntilAutoPause;
  }

  public void setSecondsUntilAutoPause(final Integer secondsUntilAutoPause) {
    this.secondsUntilAutoPause = secondsUntilAutoPause;
  }

  public String getTimeoutAction() {
    return timeoutAction;
  }

  public void setTimeoutAction(final String timeoutAction) {
    this.timeoutAction = timeoutAction;
  }

}
