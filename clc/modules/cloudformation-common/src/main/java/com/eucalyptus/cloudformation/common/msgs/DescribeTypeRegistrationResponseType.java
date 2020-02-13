/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class DescribeTypeRegistrationResponseType extends CloudFormationMessage {

  private DescribeTypeRegistrationResult result = new DescribeTypeRegistrationResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTypeRegistrationResult getDescribeTypeRegistrationResult() {
    return result;
  }

  public void setDescribeTypeRegistrationResult(final DescribeTypeRegistrationResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
