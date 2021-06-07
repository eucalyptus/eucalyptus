/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeCertificatesResponseType extends RdsMessage {

  private DescribeCertificatesResult result = new DescribeCertificatesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeCertificatesResult getDescribeCertificatesResult() {
    return result;
  }

  public void setDescribeCertificatesResult(final DescribeCertificatesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
