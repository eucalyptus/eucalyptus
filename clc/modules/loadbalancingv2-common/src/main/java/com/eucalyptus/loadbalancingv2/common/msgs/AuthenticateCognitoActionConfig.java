/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AuthenticateCognitoActionConfig extends EucalyptusData {

  private AuthenticateCognitoActionAuthenticationRequestExtraParams authenticationRequestExtraParams;

  @FieldRegex(FieldRegexValue.ENUM_AUTHENTICATECOGNITOACTIONCONDITIONALBEHAVIORENUM)
  private String onUnauthenticatedRequest;

  private String scope;

  private String sessionCookieName;

  private Long sessionTimeout;

  @Nonnull
  private String userPoolArn;

  @Nonnull
  private String userPoolClientId;

  @Nonnull
  private String userPoolDomain;

  public AuthenticateCognitoActionAuthenticationRequestExtraParams getAuthenticationRequestExtraParams() {
    return authenticationRequestExtraParams;
  }

  public void setAuthenticationRequestExtraParams(final AuthenticateCognitoActionAuthenticationRequestExtraParams authenticationRequestExtraParams) {
    this.authenticationRequestExtraParams = authenticationRequestExtraParams;
  }

  public String getOnUnauthenticatedRequest() {
    return onUnauthenticatedRequest;
  }

  public void setOnUnauthenticatedRequest(final String onUnauthenticatedRequest) {
    this.onUnauthenticatedRequest = onUnauthenticatedRequest;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(final String scope) {
    this.scope = scope;
  }

  public String getSessionCookieName() {
    return sessionCookieName;
  }

  public void setSessionCookieName(final String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
  }

  public Long getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(final Long sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public String getUserPoolArn() {
    return userPoolArn;
  }

  public void setUserPoolArn(final String userPoolArn) {
    this.userPoolArn = userPoolArn;
  }

  public String getUserPoolClientId() {
    return userPoolClientId;
  }

  public void setUserPoolClientId(final String userPoolClientId) {
    this.userPoolClientId = userPoolClientId;
  }

  public String getUserPoolDomain() {
    return userPoolDomain;
  }

  public void setUserPoolDomain(final String userPoolDomain) {
    this.userPoolDomain = userPoolDomain;
  }

}
