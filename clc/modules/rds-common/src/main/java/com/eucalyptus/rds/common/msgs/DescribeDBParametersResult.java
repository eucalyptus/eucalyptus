/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBParametersResult extends EucalyptusData {

  private String marker;

  private ParametersList parameters;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public ParametersList getParameters() {
    return parameters;
  }

  public void setParameters(final ParametersList parameters) {
    this.parameters = parameters;
  }

}
