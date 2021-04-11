/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class StopActivityStreamType extends RdsMessage {

  private Boolean applyImmediately;

  @Nonnull
  private String resourceArn;

  public Boolean getApplyImmediately() {
    return applyImmediately;
  }

  public void setApplyImmediately(final Boolean applyImmediately) {
    this.applyImmediately = applyImmediately;
  }

  public String getResourceArn() {
    return resourceArn;
  }

  public void setResourceArn(final String resourceArn) {
    this.resourceArn = resourceArn;
  }

}
