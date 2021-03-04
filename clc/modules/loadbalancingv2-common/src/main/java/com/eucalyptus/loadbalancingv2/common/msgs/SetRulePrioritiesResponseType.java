/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class SetRulePrioritiesResponseType extends Loadbalancingv2Message {

  private SetRulePrioritiesResult result = new SetRulePrioritiesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public SetRulePrioritiesResult getSetRulePrioritiesResult() {
    return result;
  }

  public void setSetRulePrioritiesResult(final SetRulePrioritiesResult result) {
    this.result = result;
  }

}
