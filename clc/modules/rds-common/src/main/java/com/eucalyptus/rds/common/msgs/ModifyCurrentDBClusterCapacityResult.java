/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyCurrentDBClusterCapacityResult extends EucalyptusData {

  private Integer currentCapacity;

  private String dBClusterIdentifier;

  private Integer pendingCapacity;

  private Integer secondsBeforeTimeout;

  private String timeoutAction;

  public Integer getCurrentCapacity() {
    return currentCapacity;
  }

  public void setCurrentCapacity(final Integer currentCapacity) {
    this.currentCapacity = currentCapacity;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public Integer getPendingCapacity() {
    return pendingCapacity;
  }

  public void setPendingCapacity(final Integer pendingCapacity) {
    this.pendingCapacity = pendingCapacity;
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
