/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;


public class ListStackInstancesType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 100)
  private Integer maxResults;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  private String stackInstanceAccount;

  private String stackInstanceRegion;

  @Nonnull
  private String stackSetName;

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(final Integer maxResults) {
    this.maxResults = maxResults;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

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
