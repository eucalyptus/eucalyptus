/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AuthenticateOidcActionAuthenticationRequestExtraParamsEntry extends EucalyptusData {

  private java.lang.String key;

  private java.lang.String value;

  public java.lang.String getKey() {
    return key;
  }

  public void setKey(final java.lang.String key) {
    this.key = key;
  }

  public java.lang.String getValue() {
    return value;
  }

  public void setValue(final java.lang.String value) {
    this.value = value;
  }
}
