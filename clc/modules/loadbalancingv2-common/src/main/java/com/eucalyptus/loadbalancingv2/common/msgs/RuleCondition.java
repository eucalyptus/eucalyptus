/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RuleCondition extends EucalyptusData {

  @FieldRange(max = 64)
  private String field;

  private HostHeaderConditionConfig hostHeaderConfig;

  private HttpHeaderConditionConfig httpHeaderConfig;

  private HttpRequestMethodConditionConfig httpRequestMethodConfig;

  private PathPatternConditionConfig pathPatternConfig;

  private QueryStringConditionConfig queryStringConfig;

  private SourceIpConditionConfig sourceIpConfig;

  private ListOfString values;

  public String getField() {
    return field;
  }

  public void setField(final String field) {
    this.field = field;
  }

  public HostHeaderConditionConfig getHostHeaderConfig() {
    return hostHeaderConfig;
  }

  public void setHostHeaderConfig(final HostHeaderConditionConfig hostHeaderConfig) {
    this.hostHeaderConfig = hostHeaderConfig;
  }

  public HttpHeaderConditionConfig getHttpHeaderConfig() {
    return httpHeaderConfig;
  }

  public void setHttpHeaderConfig(final HttpHeaderConditionConfig httpHeaderConfig) {
    this.httpHeaderConfig = httpHeaderConfig;
  }

  public HttpRequestMethodConditionConfig getHttpRequestMethodConfig() {
    return httpRequestMethodConfig;
  }

  public void setHttpRequestMethodConfig(final HttpRequestMethodConditionConfig httpRequestMethodConfig) {
    this.httpRequestMethodConfig = httpRequestMethodConfig;
  }

  public PathPatternConditionConfig getPathPatternConfig() {
    return pathPatternConfig;
  }

  public void setPathPatternConfig(final PathPatternConditionConfig pathPatternConfig) {
    this.pathPatternConfig = pathPatternConfig;
  }

  public QueryStringConditionConfig getQueryStringConfig() {
    return queryStringConfig;
  }

  public void setQueryStringConfig(final QueryStringConditionConfig queryStringConfig) {
    this.queryStringConfig = queryStringConfig;
  }

  public SourceIpConditionConfig getSourceIpConfig() {
    return sourceIpConfig;
  }

  public void setSourceIpConfig(final SourceIpConditionConfig sourceIpConfig) {
    this.sourceIpConfig = sourceIpConfig;
  }

  public ListOfString getValues() {
    return values;
  }

  public void setValues(final ListOfString values) {
    this.values = values;
  }

}
