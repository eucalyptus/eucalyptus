/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class CreateRuleResponseType extends Loadbalancingv2Message {

  private CreateRuleResult result = new CreateRuleResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateRuleResult getCreateRuleResult() {
    return result;
  }

  public void setCreateRuleResult(final CreateRuleResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
