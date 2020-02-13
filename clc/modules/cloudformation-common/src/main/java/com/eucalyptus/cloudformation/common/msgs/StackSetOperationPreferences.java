/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackSetOperationPreferences extends EucalyptusData {

  @FieldRange()
  private Integer failureToleranceCount;

  @FieldRange(max = 100)
  private Integer failureTolerancePercentage;

  @FieldRange(min = 1)
  private Integer maxConcurrentCount;

  @FieldRange(min = 1, max = 100)
  private Integer maxConcurrentPercentage;

  private RegionList regionOrder;

  public Integer getFailureToleranceCount() {
    return failureToleranceCount;
  }

  public void setFailureToleranceCount(final Integer failureToleranceCount) {
    this.failureToleranceCount = failureToleranceCount;
  }

  public Integer getFailureTolerancePercentage() {
    return failureTolerancePercentage;
  }

  public void setFailureTolerancePercentage(final Integer failureTolerancePercentage) {
    this.failureTolerancePercentage = failureTolerancePercentage;
  }

  public Integer getMaxConcurrentCount() {
    return maxConcurrentCount;
  }

  public void setMaxConcurrentCount(final Integer maxConcurrentCount) {
    this.maxConcurrentCount = maxConcurrentCount;
  }

  public Integer getMaxConcurrentPercentage() {
    return maxConcurrentPercentage;
  }

  public void setMaxConcurrentPercentage(final Integer maxConcurrentPercentage) {
    this.maxConcurrentPercentage = maxConcurrentPercentage;
  }

  public RegionList getRegionOrder() {
    return regionOrder;
  }

  public void setRegionOrder(final RegionList regionOrder) {
    this.regionOrder = regionOrder;
  }

}
