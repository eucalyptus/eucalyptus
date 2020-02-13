/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class ValidateTemplateResponseType extends CloudFormationMessage {

  private ValidateTemplateResult result = new ValidateTemplateResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public ValidateTemplateResult getValidateTemplateResult() {
    return result;
  }

  public void setValidateTemplateResult(final ValidateTemplateResult result) {
    this.result = result;
  }

}
