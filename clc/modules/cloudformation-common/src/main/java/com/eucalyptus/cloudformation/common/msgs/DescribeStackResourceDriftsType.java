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


public class DescribeStackResourceDriftsType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 100)
  private Integer maxResults;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  @Nonnull
  @FieldRange(min = 1)
  private String stackName;

  @FieldRange(min = 1, max = 4)
  private StackResourceDriftStatusFilters stackResourceDriftStatusFilters;

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

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public StackResourceDriftStatusFilters getStackResourceDriftStatusFilters() {
    return stackResourceDriftStatusFilters;
  }

  public void setStackResourceDriftStatusFilters(final StackResourceDriftStatusFilters stackResourceDriftStatusFilters) {
    this.stackResourceDriftStatusFilters = stackResourceDriftStatusFilters;
  }

}
