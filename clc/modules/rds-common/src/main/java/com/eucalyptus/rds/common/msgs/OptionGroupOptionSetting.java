/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OptionGroupOptionSetting extends EucalyptusData {

  private String allowedValues;

  private String applyType;

  private String defaultValue;

  private Boolean isModifiable;

  private Boolean isRequired;

  private MinimumEngineVersionPerAllowedValueList minimumEngineVersionPerAllowedValue;

  private String settingDescription;

  private String settingName;

  public String getAllowedValues() {
    return allowedValues;
  }

  public void setAllowedValues(final String allowedValues) {
    this.allowedValues = allowedValues;
  }

  public String getApplyType() {
    return applyType;
  }

  public void setApplyType(final String applyType) {
    this.applyType = applyType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(final String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Boolean getIsModifiable() {
    return isModifiable;
  }

  public void setIsModifiable(final Boolean isModifiable) {
    this.isModifiable = isModifiable;
  }

  public Boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(final Boolean isRequired) {
    this.isRequired = isRequired;
  }

  public MinimumEngineVersionPerAllowedValueList getMinimumEngineVersionPerAllowedValue() {
    return minimumEngineVersionPerAllowedValue;
  }

  public void setMinimumEngineVersionPerAllowedValue(final MinimumEngineVersionPerAllowedValueList minimumEngineVersionPerAllowedValue) {
    this.minimumEngineVersionPerAllowedValue = minimumEngineVersionPerAllowedValue;
  }

  public String getSettingDescription() {
    return settingDescription;
  }

  public void setSettingDescription(final String settingDescription) {
    this.settingDescription = settingDescription;
  }

  public String getSettingName() {
    return settingName;
  }

  public void setSettingName(final String settingName) {
    this.settingName = settingName;
  }

}
