/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ResetDBParameterGroupType extends RdsMessage {

  @Nonnull
  private String dBParameterGroupName;

  private ParametersList parameters;

  private Boolean resetAllParameters;

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public ParametersList getParameters() {
    return parameters;
  }

  public void setParameters(final ParametersList parameters) {
    this.parameters = parameters;
  }

  public Boolean getResetAllParameters() {
    return resetAllParameters;
  }

  public void setResetAllParameters(final Boolean resetAllParameters) {
    this.resetAllParameters = resetAllParameters;
  }

}
