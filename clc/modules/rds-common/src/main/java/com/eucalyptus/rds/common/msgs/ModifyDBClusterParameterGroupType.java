/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBClusterParameterGroupType extends RdsMessage {

  @Nonnull
  private String dBClusterParameterGroupName;

  @Nonnull
  private ParametersList parameters;

  public String getDBClusterParameterGroupName() {
    return dBClusterParameterGroupName;
  }

  public void setDBClusterParameterGroupName(final String dBClusterParameterGroupName) {
    this.dBClusterParameterGroupName = dBClusterParameterGroupName;
  }

  public ParametersList getParameters() {
    return parameters;
  }

  public void setParameters(final ParametersList parameters) {
    this.parameters = parameters;
  }

}
