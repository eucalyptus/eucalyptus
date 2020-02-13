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


public class DetectStackSetDriftType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 128)
  private String operationId;

  private StackSetOperationPreferences operationPreferences;

  @Nonnull
  private String stackSetName;

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(final String operationId) {
    this.operationId = operationId;
  }

  public StackSetOperationPreferences getOperationPreferences() {
    return operationPreferences;
  }

  public void setOperationPreferences(final StackSetOperationPreferences operationPreferences) {
    this.operationPreferences = operationPreferences;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

}
