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


public class DeleteStackInstancesType extends CloudFormationMessage {

  @Nonnull
  private AccountList accounts;

  @FieldRange(min = 1, max = 128)
  private String operationId;

  private StackSetOperationPreferences operationPreferences;

  @Nonnull
  private RegionList regions;

  @Nonnull
  private Boolean retainStacks;

  @Nonnull
  private String stackSetName;

  public AccountList getAccounts() {
    return accounts;
  }

  public void setAccounts(final AccountList accounts) {
    this.accounts = accounts;
  }

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

  public RegionList getRegions() {
    return regions;
  }

  public void setRegions(final RegionList regions) {
    this.regions = regions;
  }

  public Boolean getRetainStacks() {
    return retainStacks;
  }

  public void setRetainStacks(final Boolean retainStacks) {
    this.retainStacks = retainStacks;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

}
