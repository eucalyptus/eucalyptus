/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterSnapshotsResponseType extends RdsMessage {

  private DescribeDBClusterSnapshotsResult result = new DescribeDBClusterSnapshotsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterSnapshotsResult getDescribeDBClusterSnapshotsResult() {
    return result;
  }

  public void setDescribeDBClusterSnapshotsResult(final DescribeDBClusterSnapshotsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
