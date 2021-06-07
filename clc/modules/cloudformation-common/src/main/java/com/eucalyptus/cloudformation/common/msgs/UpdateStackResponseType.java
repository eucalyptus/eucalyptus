/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class UpdateStackResponseType extends CloudFormationMessage {

  private UpdateStackResult result = new UpdateStackResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public UpdateStackResult getUpdateStackResult() {
    return result;
  }

  public void setUpdateStackResult(final UpdateStackResult result) {
    this.result = result;
  }

}
