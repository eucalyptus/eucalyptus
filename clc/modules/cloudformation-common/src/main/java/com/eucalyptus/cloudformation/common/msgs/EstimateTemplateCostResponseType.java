/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class EstimateTemplateCostResponseType extends CloudFormationMessage {

  private EstimateTemplateCostResult result = new EstimateTemplateCostResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public EstimateTemplateCostResult getEstimateTemplateCostResult() {
    return result;
  }

  public void setEstimateTemplateCostResult(final EstimateTemplateCostResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
