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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Action extends EucalyptusData {

  private AuthenticateCognitoActionConfig authenticateCognitoConfig;

  private AuthenticateOidcActionConfig authenticateOidcConfig;

  private FixedResponseActionConfig fixedResponseConfig;

  private ForwardActionConfig forwardConfig;

  @FieldRange(min = 1, max = 50000)
  private Integer order;

  private RedirectActionConfig redirectConfig;

  @FieldRegex(FieldRegexValue.LOADBALANCING_ARN)
  private String targetGroupArn;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_ACTIONTYPEENUM)
  private String type;

  public AuthenticateCognitoActionConfig getAuthenticateCognitoConfig() {
    return authenticateCognitoConfig;
  }

  public void setAuthenticateCognitoConfig(final AuthenticateCognitoActionConfig authenticateCognitoConfig) {
    this.authenticateCognitoConfig = authenticateCognitoConfig;
  }

  public AuthenticateOidcActionConfig getAuthenticateOidcConfig() {
    return authenticateOidcConfig;
  }

  public void setAuthenticateOidcConfig(final AuthenticateOidcActionConfig authenticateOidcConfig) {
    this.authenticateOidcConfig = authenticateOidcConfig;
  }

  public FixedResponseActionConfig getFixedResponseConfig() {
    return fixedResponseConfig;
  }

  public void setFixedResponseConfig(final FixedResponseActionConfig fixedResponseConfig) {
    this.fixedResponseConfig = fixedResponseConfig;
  }

  public ForwardActionConfig getForwardConfig() {
    return forwardConfig;
  }

  public void setForwardConfig(final ForwardActionConfig forwardConfig) {
    this.forwardConfig = forwardConfig;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(final Integer order) {
    this.order = order;
  }

  public RedirectActionConfig getRedirectConfig() {
    return redirectConfig;
  }

  public void setRedirectConfig(final RedirectActionConfig redirectConfig) {
    this.redirectConfig = redirectConfig;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
