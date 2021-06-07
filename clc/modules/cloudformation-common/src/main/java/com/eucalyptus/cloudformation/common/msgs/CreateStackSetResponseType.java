/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class CreateStackSetResponseType extends CloudFormationMessage {

  private CreateStackSetResult result = new CreateStackSetResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateStackSetResult getCreateStackSetResult() {
    return result;
  }

  public void setCreateStackSetResult(final CreateStackSetResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
