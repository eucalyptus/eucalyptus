/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class ModifyRuleResponseType extends Loadbalancingv2Message {

  private ModifyRuleResult result = new ModifyRuleResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyRuleResult getModifyRuleResult() {
    return result;
  }

  public void setModifyRuleResult(final ModifyRuleResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
