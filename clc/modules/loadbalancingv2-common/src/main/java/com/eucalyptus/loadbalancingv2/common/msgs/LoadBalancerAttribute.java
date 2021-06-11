/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadBalancerAttribute extends EucalyptusData {

  @FieldRange(max = 256)
  private String key;

  @FieldRange(max = 1024)
  private String value;

  public static LoadBalancerAttribute of(final String key, final String value) {
    final LoadBalancerAttribute attribute = new LoadBalancerAttribute();
    attribute.setKey(key);
    attribute.setValue(value);
    return attribute;
  }

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
