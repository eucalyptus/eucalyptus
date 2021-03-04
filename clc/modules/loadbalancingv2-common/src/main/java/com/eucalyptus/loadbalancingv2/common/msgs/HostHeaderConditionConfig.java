/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HostHeaderConditionConfig extends EucalyptusData {

  private ListOfString values;

  public ListOfString getValues() {
    return values;
  }

  public void setValues(final ListOfString values) {
    this.values = values;
  }

}
