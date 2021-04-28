/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Tag extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String key;

  @FieldRange(max = 256)
  private String value;

  public String getKey() {
    return key;
  }

  public void setKey(final String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

}
