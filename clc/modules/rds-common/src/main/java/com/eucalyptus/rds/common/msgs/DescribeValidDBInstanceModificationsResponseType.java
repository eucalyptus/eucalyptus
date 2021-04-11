/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeValidDBInstanceModificationsResponseType extends RdsMessage {

  private DescribeValidDBInstanceModificationsResult result = new DescribeValidDBInstanceModificationsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeValidDBInstanceModificationsResult getDescribeValidDBInstanceModificationsResult() {
    return result;
  }

  public void setDescribeValidDBInstanceModificationsResult(final DescribeValidDBInstanceModificationsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
