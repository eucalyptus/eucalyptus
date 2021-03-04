/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class QueryStringConditionConfig extends EucalyptusData {

  private QueryStringKeyValuePairList values;

  public QueryStringKeyValuePairList getValues() {
    return values;
  }

  public void setValues(final QueryStringKeyValuePairList values) {
    this.values = values;
  }

}
