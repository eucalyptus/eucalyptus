/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeCustomAvailabilityZonesResponseType extends RdsMessage {

  private DescribeCustomAvailabilityZonesResult result = new DescribeCustomAvailabilityZonesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeCustomAvailabilityZonesResult getDescribeCustomAvailabilityZonesResult() {
    return result;
  }

  public void setDescribeCustomAvailabilityZonesResult(final DescribeCustomAvailabilityZonesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
