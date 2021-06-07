/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class DescribeTypeResponseType extends CloudFormationMessage {

  private DescribeTypeResult result = new DescribeTypeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTypeResult getDescribeTypeResult() {
    return result;
  }

  public void setDescribeTypeResult(final DescribeTypeResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
