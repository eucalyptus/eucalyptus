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


public class ContinueUpdateRollbackType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  private ResourcesToSkip resourcesToSkip;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  @Nonnull
  @FieldRange(min = 1)
  private String stackName;

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public ResourcesToSkip getResourcesToSkip() {
    return resourcesToSkip;
  }

  public void setResourcesToSkip(final ResourcesToSkip resourcesToSkip) {
    this.resourcesToSkip = resourcesToSkip;
  }

  public String getRoleARN() {
    return roleARN;
  }

  public void setRoleARN(final String roleARN) {
    this.roleARN = roleARN;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

}
