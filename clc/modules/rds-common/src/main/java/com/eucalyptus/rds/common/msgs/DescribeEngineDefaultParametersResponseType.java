/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEngineDefaultParametersResponseType extends RdsMessage {

  private DescribeEngineDefaultParametersResult result = new DescribeEngineDefaultParametersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeEngineDefaultParametersResult getDescribeEngineDefaultParametersResult() {
    return result;
  }

  public void setDescribeEngineDefaultParametersResult(final DescribeEngineDefaultParametersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
