/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBInstanceAutomatedBackupsResponseType extends RdsMessage {

  private DescribeDBInstanceAutomatedBackupsResult result = new DescribeDBInstanceAutomatedBackupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBInstanceAutomatedBackupsResult getDescribeDBInstanceAutomatedBackupsResult() {
    return result;
  }

  public void setDescribeDBInstanceAutomatedBackupsResult(final DescribeDBInstanceAutomatedBackupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
