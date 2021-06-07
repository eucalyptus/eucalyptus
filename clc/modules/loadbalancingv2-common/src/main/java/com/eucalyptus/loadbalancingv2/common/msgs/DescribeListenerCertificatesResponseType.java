/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeListenerCertificatesResponseType extends Loadbalancingv2Message {

  private DescribeListenerCertificatesResult result = new DescribeListenerCertificatesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeListenerCertificatesResult getDescribeListenerCertificatesResult() {
    return result;
  }

  public void setDescribeListenerCertificatesResult(final DescribeListenerCertificatesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
