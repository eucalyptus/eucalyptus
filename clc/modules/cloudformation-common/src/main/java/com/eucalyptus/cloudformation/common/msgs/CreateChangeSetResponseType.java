/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class CreateChangeSetResponseType extends CloudFormationMessage {

  private CreateChangeSetResult result = new CreateChangeSetResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateChangeSetResult getCreateChangeSetResult() {
    return result;
  }

  public void setCreateChangeSetResult(final CreateChangeSetResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
