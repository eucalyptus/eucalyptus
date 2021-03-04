/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;


public class CreateListenerType extends Loadbalancingv2Message {

  private CertificateList certificates;

  @Nonnull
  private Actions defaultActions;

  @Nonnull
  private String loadBalancerArn;

  @Nonnull
  @FieldRange(min = 1, max = 65535)
  private Integer port;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_PROTOCOLENUM)
  private String protocol;

  private String sslPolicy;

  public CertificateList getCertificates() {
    return certificates;
  }

  public void setCertificates(final CertificateList certificates) {
    this.certificates = certificates;
  }

  public Actions getDefaultActions() {
    return defaultActions;
  }

  public void setDefaultActions(final Actions defaultActions) {
    this.defaultActions = defaultActions;
  }

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(final String protocol) {
    this.protocol = protocol;
  }

  public String getSslPolicy() {
    return sslPolicy;
  }

  public void setSslPolicy(final String sslPolicy) {
    this.sslPolicy = sslPolicy;
  }

}
