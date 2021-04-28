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


public class AuthenticateOidcActionConfig extends EucalyptusData {

  private AuthenticateOidcActionAuthenticationRequestExtraParams authenticationRequestExtraParams;

  @Nonnull
  private String authorizationEndpoint;

  @Nonnull
  private String clientId;

  private String clientSecret;

  @Nonnull
  private String issuer;

  @FieldRegex(FieldRegexValue.ENUM_AUTHENTICATEOIDCACTIONCONDITIONALBEHAVIORENUM)
  private String onUnauthenticatedRequest;

  private String scope;

  private String sessionCookieName;

  private Long sessionTimeout;

  @Nonnull
  private String tokenEndpoint;

  private Boolean useExistingClientSecret;

  @Nonnull
  private String userInfoEndpoint;

  public AuthenticateOidcActionAuthenticationRequestExtraParams getAuthenticationRequestExtraParams() {
    return authenticationRequestExtraParams;
  }

  public void setAuthenticationRequestExtraParams(final AuthenticateOidcActionAuthenticationRequestExtraParams authenticationRequestExtraParams) {
    this.authenticationRequestExtraParams = authenticationRequestExtraParams;
  }

  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(final String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(final String issuer) {
    this.issuer = issuer;
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

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(final String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public Boolean getUseExistingClientSecret() {
    return useExistingClientSecret;
  }

  public void setUseExistingClientSecret(final Boolean useExistingClientSecret) {
    this.useExistingClientSecret = useExistingClientSecret;
  }

  public String getUserInfoEndpoint() {
    return userInfoEndpoint;
  }

  public void setUserInfoEndpoint(final String userInfoEndpoint) {
    this.userInfoEndpoint = userInfoEndpoint;
  }

}
