/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadBalancerState extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_LOADBALANCERSTATEENUM)
  private String code;

  private String reason;

  public String getCode() {
    return code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(final String reason) {
    this.reason = reason;
  }

}
