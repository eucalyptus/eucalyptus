/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class RemoveListenerCertificatesResponseType extends Loadbalancingv2Message {

  private RemoveListenerCertificatesResult result = new RemoveListenerCertificatesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RemoveListenerCertificatesResult getRemoveListenerCertificatesResult() {
    return result;
  }

  public void setRemoveListenerCertificatesResult(final RemoveListenerCertificatesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
