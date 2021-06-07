/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBSubnetGroupResponseType extends RdsMessage {

  private ModifyDBSubnetGroupResult result = new ModifyDBSubnetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBSubnetGroupResult getModifyDBSubnetGroupResult() {
    return result;
  }

  public void setModifyDBSubnetGroupResult(final ModifyDBSubnetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
