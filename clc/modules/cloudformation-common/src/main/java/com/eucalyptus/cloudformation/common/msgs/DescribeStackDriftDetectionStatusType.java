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


public class DescribeStackDriftDetectionStatusType extends CloudFormationMessage {

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String stackDriftDetectionId;

  public String getStackDriftDetectionId() {
    return stackDriftDetectionId;
  }

  public void setStackDriftDetectionId(final String stackDriftDetectionId) {
    this.stackDriftDetectionId = stackDriftDetectionId;
  }

}
