/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyOptionGroupType extends RdsMessage {

  private Boolean applyImmediately;

  @Nonnull
  private String optionGroupName;

  private OptionConfigurationList optionsToInclude;

  private OptionNamesList optionsToRemove;

  public Boolean getApplyImmediately() {
    return applyImmediately;
  }

  public void setApplyImmediately(final Boolean applyImmediately) {
    this.applyImmediately = applyImmediately;
  }

  public String getOptionGroupName() {
    return optionGroupName;
  }

  public void setOptionGroupName(final String optionGroupName) {
    this.optionGroupName = optionGroupName;
  }

  public OptionConfigurationList getOptionsToInclude() {
    return optionsToInclude;
  }

  public void setOptionsToInclude(final OptionConfigurationList optionsToInclude) {
    this.optionsToInclude = optionsToInclude;
  }

  public OptionNamesList getOptionsToRemove() {
    return optionsToRemove;
  }

  public void setOptionsToRemove(final OptionNamesList optionsToRemove) {
    this.optionsToRemove = optionsToRemove;
  }

}
