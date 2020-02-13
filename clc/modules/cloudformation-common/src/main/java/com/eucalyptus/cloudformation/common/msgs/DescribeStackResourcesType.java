/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

public class DescribeStackResourcesType extends CloudFormationMessage {

  private String logicalResourceId;

  private String physicalResourceId;

  private String stackName;

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(final String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getPhysicalResourceId() {
    return physicalResourceId;
  }

  public void setPhysicalResourceId(final String physicalResourceId) {
    this.physicalResourceId = physicalResourceId;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

}
