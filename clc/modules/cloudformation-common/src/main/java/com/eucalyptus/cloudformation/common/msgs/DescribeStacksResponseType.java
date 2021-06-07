/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class DescribeStacksResponseType extends CloudFormationMessage {

  private DescribeStacksResult result = new DescribeStacksResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeStacksResult getDescribeStacksResult() {
    return result;
  }

  public void setDescribeStacksResult(final DescribeStacksResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
