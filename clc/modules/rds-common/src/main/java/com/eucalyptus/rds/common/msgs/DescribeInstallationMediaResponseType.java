/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeInstallationMediaResponseType extends RdsMessage {

  private DescribeInstallationMediaResult result = new DescribeInstallationMediaResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeInstallationMediaResult getDescribeInstallationMediaResult() {
    return result;
  }

  public void setDescribeInstallationMediaResult(final DescribeInstallationMediaResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
