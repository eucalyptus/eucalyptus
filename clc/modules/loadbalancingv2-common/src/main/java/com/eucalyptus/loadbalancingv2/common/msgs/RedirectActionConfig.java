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


public class RedirectActionConfig extends EucalyptusData {

  @FieldRange(min = 1, max = 128)
  private String host;

  @FieldRange(min = 1, max = 128)
  private String path;

  @FieldRegex(FieldRegexValue.REDIRECT_PORT)
  private String port;

  @FieldRegex(FieldRegexValue.REDIRECT_PROTOCOL)
  private String protocol;

  @FieldRange(max = 128)
  private String query;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_REDIRECTACTIONSTATUSCODEENUM)
  private String statusCode;

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  public String getPort() {
    return port;
  }

  public void setPort(final String port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(final String protocol) {
    this.protocol = protocol;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(final String query) {
    this.query = query;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(final String statusCode) {
    this.statusCode = statusCode;
  }

}
