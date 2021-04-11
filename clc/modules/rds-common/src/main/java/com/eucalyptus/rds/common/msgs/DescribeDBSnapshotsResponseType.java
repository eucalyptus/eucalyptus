/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBSnapshotsResponseType extends RdsMessage {

  private DescribeDBSnapshotsResult result = new DescribeDBSnapshotsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBSnapshotsResult getDescribeDBSnapshotsResult() {
    return result;
  }

  public void setDescribeDBSnapshotsResult(final DescribeDBSnapshotsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
