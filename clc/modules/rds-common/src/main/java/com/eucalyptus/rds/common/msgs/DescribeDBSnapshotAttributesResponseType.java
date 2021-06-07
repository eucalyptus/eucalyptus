/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBSnapshotAttributesResponseType extends RdsMessage {

  private DescribeDBSnapshotAttributesResult result = new DescribeDBSnapshotAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBSnapshotAttributesResult getDescribeDBSnapshotAttributesResult() {
    return result;
  }

  public void setDescribeDBSnapshotAttributesResult(final DescribeDBSnapshotAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
