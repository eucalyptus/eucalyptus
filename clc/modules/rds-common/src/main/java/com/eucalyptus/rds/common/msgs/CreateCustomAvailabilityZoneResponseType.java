/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateCustomAvailabilityZoneResponseType extends RdsMessage {

  private CreateCustomAvailabilityZoneResult result = new CreateCustomAvailabilityZoneResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateCustomAvailabilityZoneResult getCreateCustomAvailabilityZoneResult() {
    return result;
  }

  public void setCreateCustomAvailabilityZoneResult(final CreateCustomAvailabilityZoneResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
