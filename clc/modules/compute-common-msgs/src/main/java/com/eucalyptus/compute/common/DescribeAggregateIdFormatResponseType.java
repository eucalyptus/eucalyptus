/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeAggregateIdFormatResponseType extends ComputeMessage {


  private IdFormatList statuses;
  private Boolean useLongIdsAggregated;

  public IdFormatList getStatuses( ) {
    return statuses;
  }

  public void setStatuses( final IdFormatList statuses ) {
    this.statuses = statuses;
  }

  public Boolean getUseLongIdsAggregated( ) {
    return useLongIdsAggregated;
  }

  public void setUseLongIdsAggregated( final Boolean useLongIdsAggregated ) {
    this.useLongIdsAggregated = useLongIdsAggregated;
  }

}
