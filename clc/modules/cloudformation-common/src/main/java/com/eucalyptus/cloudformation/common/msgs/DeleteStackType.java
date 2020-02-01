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


public class DeleteStackType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  private RetainResources retainResources;

  @FieldRange(min = 20, max = 2048)
  private String roleARN;

  @Nonnull
  private String stackName;

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public RetainResources getRetainResources() {
    return retainResources;
  }

  public void setRetainResources(final RetainResources retainResources) {
    this.retainResources = retainResources;
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
