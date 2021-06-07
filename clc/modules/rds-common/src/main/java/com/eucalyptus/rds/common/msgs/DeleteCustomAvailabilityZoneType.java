/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DeleteCustomAvailabilityZoneType extends RdsMessage {

  @Nonnull
  private String customAvailabilityZoneId;

  public String getCustomAvailabilityZoneId() {
    return customAvailabilityZoneId;
  }

  public void setCustomAvailabilityZoneId(final String customAvailabilityZoneId) {
    this.customAvailabilityZoneId = customAvailabilityZoneId;
  }

}
