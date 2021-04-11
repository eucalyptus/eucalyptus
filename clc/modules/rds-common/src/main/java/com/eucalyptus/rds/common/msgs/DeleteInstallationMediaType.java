/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DeleteInstallationMediaType extends RdsMessage {

  @Nonnull
  private String installationMediaId;

  public String getInstallationMediaId() {
    return installationMediaId;
  }

  public void setInstallationMediaId(final String installationMediaId) {
    this.installationMediaId = installationMediaId;
  }

}
