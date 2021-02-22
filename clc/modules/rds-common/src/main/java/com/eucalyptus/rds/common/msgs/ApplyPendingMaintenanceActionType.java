/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ApplyPendingMaintenanceActionType extends RdsMessage {

  @Nonnull
  private String applyAction;

  @Nonnull
  private String optInType;

  @Nonnull
  private String resourceIdentifier;

  public String getApplyAction() {
    return applyAction;
  }

  public void setApplyAction(final String applyAction) {
    this.applyAction = applyAction;
  }

  public String getOptInType() {
    return optInType;
  }

  public void setOptInType(final String optInType) {
    this.optInType = optInType;
  }

  public String getResourceIdentifier() {
    return resourceIdentifier;
  }

  public void setResourceIdentifier(final String resourceIdentifier) {
    this.resourceIdentifier = resourceIdentifier;
  }

}
