/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;


public class DescribeStackInstanceType extends CloudFormationMessage {

  @Nonnull
  private String stackInstanceAccount;

  @Nonnull
  private String stackInstanceRegion;

  @Nonnull
  private String stackSetName;

  public String getStackInstanceAccount() {
    return stackInstanceAccount;
  }

  public void setStackInstanceAccount(final String stackInstanceAccount) {
    this.stackInstanceAccount = stackInstanceAccount;
  }

  public String getStackInstanceRegion() {
    return stackInstanceRegion;
  }

  public void setStackInstanceRegion(final String stackInstanceRegion) {
    this.stackInstanceRegion = stackInstanceRegion;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

}
