/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBSubnetGroupsResponseType extends RdsMessage {

  private DescribeDBSubnetGroupsResult result = new DescribeDBSubnetGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBSubnetGroupsResult getDescribeDBSubnetGroupsResult() {
    return result;
  }

  public void setDescribeDBSubnetGroupsResult(final DescribeDBSubnetGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
