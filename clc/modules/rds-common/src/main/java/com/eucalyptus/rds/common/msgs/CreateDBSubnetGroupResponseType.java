/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBSubnetGroupResponseType extends RdsMessage {

  private CreateDBSubnetGroupResult result = new CreateDBSubnetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBSubnetGroupResult getCreateDBSubnetGroupResult() {
    return result;
  }

  public void setCreateDBSubnetGroupResult(final CreateDBSubnetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
