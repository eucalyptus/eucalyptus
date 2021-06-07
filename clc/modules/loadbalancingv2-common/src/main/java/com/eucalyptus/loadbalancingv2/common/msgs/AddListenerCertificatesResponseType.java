/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class AddListenerCertificatesResponseType extends Loadbalancingv2Message {

  private AddListenerCertificatesResult result = new AddListenerCertificatesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public AddListenerCertificatesResult getAddListenerCertificatesResult() {
    return result;
  }

  public void setAddListenerCertificatesResult(final AddListenerCertificatesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
