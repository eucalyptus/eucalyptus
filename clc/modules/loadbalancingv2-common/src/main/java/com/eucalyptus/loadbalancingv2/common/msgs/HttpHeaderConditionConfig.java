/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HttpHeaderConditionConfig extends EucalyptusData {

  private String httpHeaderName;

  private ListOfString values;

  public String getHttpHeaderName() {
    return httpHeaderName;
  }

  public void setHttpHeaderName(final String httpHeaderName) {
    this.httpHeaderName = httpHeaderName;
  }

  public ListOfString getValues() {
    return values;
  }

  public void setValues(final ListOfString values) {
    this.values = values;
  }

}
