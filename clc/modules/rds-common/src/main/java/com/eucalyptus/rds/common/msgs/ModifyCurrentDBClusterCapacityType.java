/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyCurrentDBClusterCapacityType extends RdsMessage {

  private Integer capacity;

  @Nonnull
  private String dBClusterIdentifier;

  private Integer secondsBeforeTimeout;

  private String timeoutAction;

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(final Integer capacity) {
    this.capacity = capacity;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public Integer getSecondsBeforeTimeout() {
    return secondsBeforeTimeout;
  }

  public void setSecondsBeforeTimeout(final Integer secondsBeforeTimeout) {
    this.secondsBeforeTimeout = secondsBeforeTimeout;
  }

  public String getTimeoutAction() {
    return timeoutAction;
  }

  public void setTimeoutAction(final String timeoutAction) {
    this.timeoutAction = timeoutAction;
  }

}
