/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OptionSetting extends EucalyptusData {

  private String allowedValues;

  private String applyType;

  private String dataType;

  private String defaultValue;

  private String description;

  private Boolean isCollection;

  private Boolean isModifiable;

  private String name;

  private String value;

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

  public String getDataType() {
    return dataType;
  }

  public void setDataType(final String dataType) {
    this.dataType = dataType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(final String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Boolean getIsCollection() {
    return isCollection;
  }

  public void setIsCollection(final Boolean isCollection) {
    this.isCollection = isCollection;
  }

  public Boolean getIsModifiable() {
    return isModifiable;
  }

  public void setIsModifiable(final Boolean isModifiable) {
    this.isModifiable = isModifiable;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

}
