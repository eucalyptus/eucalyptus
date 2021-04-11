/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class RebootDBInstanceType extends RdsMessage {

  @Nonnull
  private String dBInstanceIdentifier;

  private Boolean forceFailover;

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public Boolean getForceFailover() {
    return forceFailover;
  }

  public void setForceFailover(final Boolean forceFailover) {
    this.forceFailover = forceFailover;
  }

}
