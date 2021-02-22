/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeReservedDBInstancesOfferingsResponseType extends RdsMessage {

  private DescribeReservedDBInstancesOfferingsResult result = new DescribeReservedDBInstancesOfferingsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeReservedDBInstancesOfferingsResult getDescribeReservedDBInstancesOfferingsResult() {
    return result;
  }

  public void setDescribeReservedDBInstancesOfferingsResult(final DescribeReservedDBInstancesOfferingsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
