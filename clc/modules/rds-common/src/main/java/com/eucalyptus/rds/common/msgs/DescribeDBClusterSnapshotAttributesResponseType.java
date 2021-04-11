/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterSnapshotAttributesResponseType extends RdsMessage {

  private DescribeDBClusterSnapshotAttributesResult result = new DescribeDBClusterSnapshotAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterSnapshotAttributesResult getDescribeDBClusterSnapshotAttributesResult() {
    return result;
  }

  public void setDescribeDBClusterSnapshotAttributesResult(final DescribeDBClusterSnapshotAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
