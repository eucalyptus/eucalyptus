/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.rds.common.msgs.Parameter;
import java.util.ArrayList;

public class DefaultDBGroupParameters {

  private ArrayList<Parameter> parameters = new ArrayList<>();

  public ArrayList<Parameter> getParameters() {
    return parameters;
  }

  public void setParameters(ArrayList<Parameter> parameters) {
    this.parameters = parameters;
  }
}
