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


public class UpdateTerminationProtectionType extends CloudFormationMessage {

  @Nonnull
  private Boolean enableTerminationProtection;

  @Nonnull
  @FieldRange(min = 1)
  private String stackName;

  public Boolean getEnableTerminationProtection() {
    return enableTerminationProtection;
  }

  public void setEnableTerminationProtection(final Boolean enableTerminationProtection) {
    this.enableTerminationProtection = enableTerminationProtection;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

}
