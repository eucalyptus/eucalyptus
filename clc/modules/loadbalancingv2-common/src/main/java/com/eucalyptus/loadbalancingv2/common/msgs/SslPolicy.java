/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class SslPolicy extends EucalyptusData {

  private Ciphers ciphers;

  private String name;

  private SslProtocols sslProtocols;

  public Ciphers getCiphers() {
    return ciphers;
  }

  public void setCiphers(final Ciphers ciphers) {
    this.ciphers = ciphers;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public SslProtocols getSslProtocols() {
    return sslProtocols;
  }

  public void setSslProtocols(final SslProtocols sslProtocols) {
    this.sslProtocols = sslProtocols;
  }

}
