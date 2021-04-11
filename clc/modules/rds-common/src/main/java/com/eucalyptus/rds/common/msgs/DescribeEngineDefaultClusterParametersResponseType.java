/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEngineDefaultClusterParametersResponseType extends RdsMessage {

  private DescribeEngineDefaultClusterParametersResult result = new DescribeEngineDefaultClusterParametersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeEngineDefaultClusterParametersResult getDescribeEngineDefaultClusterParametersResult() {
    return result;
  }

  public void setDescribeEngineDefaultClusterParametersResult(final DescribeEngineDefaultClusterParametersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
