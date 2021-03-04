/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DeleteRuleResponseType extends Loadbalancingv2Message {

  private DeleteRuleResult result = new DeleteRuleResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteRuleResult getDeleteRuleResult() {
    return result;
  }

  public void setDeleteRuleResult(final DeleteRuleResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
