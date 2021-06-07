/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class SetIpAddressTypeResponseType extends Loadbalancingv2Message {

  private SetIpAddressTypeResult result = new SetIpAddressTypeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public SetIpAddressTypeResult getSetIpAddressTypeResult() {
    return result;
  }

  public void setSetIpAddressTypeResult(final SetIpAddressTypeResult result) {
    this.result = result;
  }

}
