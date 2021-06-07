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


public class CreateStackInstancesType extends CloudFormationMessage {

  @Nonnull
  private AccountList accounts;

  @FieldRange(min = 1, max = 128)
  private String operationId;

  private StackSetOperationPreferences operationPreferences;

  private Parameters parameterOverrides;

  @Nonnull
  private RegionList regions;

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

  public Parameters getParameterOverrides() {
    return parameterOverrides;
  }

  public void setParameterOverrides(final Parameters parameterOverrides) {
    this.parameterOverrides = parameterOverrides;
  }

  public RegionList getRegions() {
    return regions;
  }

  public void setRegions(final RegionList regions) {
    this.regions = regions;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

}
