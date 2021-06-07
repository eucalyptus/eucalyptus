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


public class SetStackPolicyType extends CloudFormationMessage {

  @Nonnull
  private String stackName;

  @FieldRange(min = 1, max = 16384)
  private String stackPolicyBody;

  @FieldRange(min = 1, max = 1350)
  private String stackPolicyURL;

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public String getStackPolicyBody() {
    return stackPolicyBody;
  }

  public void setStackPolicyBody(final String stackPolicyBody) {
    this.stackPolicyBody = stackPolicyBody;
  }

  public String getStackPolicyURL() {
    return stackPolicyURL;
  }

  public void setStackPolicyURL(final String stackPolicyURL) {
    this.stackPolicyURL = stackPolicyURL;
  }

}
