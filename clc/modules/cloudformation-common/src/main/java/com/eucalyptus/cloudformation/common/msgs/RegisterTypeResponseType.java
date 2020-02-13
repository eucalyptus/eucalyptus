/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class RegisterTypeResponseType extends CloudFormationMessage {

  private RegisterTypeResult result = new RegisterTypeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RegisterTypeResult getRegisterTypeResult() {
    return result;
  }

  public void setRegisterTypeResult(final RegisterTypeResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
