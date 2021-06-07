/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class UpdateStackSetResponseType extends CloudFormationMessage {

  private UpdateStackSetResult result = new UpdateStackSetResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public UpdateStackSetResult getUpdateStackSetResult() {
    return result;
  }

  public void setUpdateStackSetResult(final UpdateStackSetResult result) {
    this.result = result;
  }

}
