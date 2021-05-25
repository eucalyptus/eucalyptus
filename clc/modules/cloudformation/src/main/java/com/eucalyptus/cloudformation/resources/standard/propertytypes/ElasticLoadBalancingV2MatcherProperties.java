/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2MatcherProperties {

  @Property
  private String grpcCode;

  @Property
  private String httpCode;

  public String getGrpcCode() {
    return grpcCode;
  }

  public void setGrpcCode(String grpcCode) {
    this.grpcCode = grpcCode;
  }

  public String getHttpCode() {
    return httpCode;
  }

  public void setHttpCode(String httpCode) {
    this.httpCode = httpCode;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("grpcCode", grpcCode)
        .add("httpCode", httpCode)
        .toString();
  }
}
