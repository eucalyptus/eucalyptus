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


public class ExecuteChangeSetType extends CloudFormationMessage {

  @Nonnull
  @FieldRange(min = 1, max = 1600)
  private String changeSetName;

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @FieldRange(min = 1)
  private String stackName;

  public String getChangeSetName() {
    return changeSetName;
  }

  public void setChangeSetName(final String changeSetName) {
    this.changeSetName = changeSetName;
  }

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

}
