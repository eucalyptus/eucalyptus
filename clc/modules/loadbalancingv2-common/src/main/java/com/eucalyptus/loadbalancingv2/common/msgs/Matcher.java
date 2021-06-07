/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Matcher extends EucalyptusData {

  @FieldRegex(FieldRegexValue.CODE_VALUES_OR_RANGE)
  private String grpcCode;

  @FieldRegex(FieldRegexValue.CODE_VALUES_OR_RANGE)
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

  public void setHttpCode(final String httpCode) {
    this.httpCode = httpCode;
  }

}
