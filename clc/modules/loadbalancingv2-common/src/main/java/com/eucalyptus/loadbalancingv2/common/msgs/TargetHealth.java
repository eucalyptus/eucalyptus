/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetHealth extends EucalyptusData {

  private String description;

  @FieldRegex(FieldRegexValue.ENUM_TARGETHEALTHREASONENUM)
  private String reason;

  @FieldRegex(FieldRegexValue.ENUM_TARGETHEALTHSTATEENUM)
  private String state;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(final String reason) {
    this.reason = reason;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
  }

}
