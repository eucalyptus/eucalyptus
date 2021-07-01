/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Parameter extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ESTRING_4096)
  private String allowedValues;

  @FieldRegex(FieldRegexValue.ENUM_APPLYMETHOD)
  private String applyMethod;

  @FieldRegex(FieldRegexValue.ESTRING_255)
  private String applyType;

  @FieldRegex(FieldRegexValue.ESTRING_255)
  private String dataType;

  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private String description;

  private Boolean isModifiable;

  @FieldRegex(FieldRegexValue.ESTRING_255)
  private String minimumEngineVersion;

  @FieldRegex(FieldRegexValue.STRING_255)
  private String parameterName;

  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private String parameterValue;

  @FieldRegex(FieldRegexValue.ENUM_SOURCE)
  private String source;

  private EngineModeList supportedEngineModes;

  public String getAllowedValues() {
    return allowedValues;
  }

  public void setAllowedValues(final String allowedValues) {
    this.allowedValues = allowedValues;
  }

  public String getApplyMethod() {
    return applyMethod;
  }

  public void setApplyMethod(final String applyMethod) {
    this.applyMethod = applyMethod;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Boolean getIsModifiable() {
    return isModifiable;
  }

  public void setIsModifiable(final Boolean isModifiable) {
    this.isModifiable = isModifiable;
  }

  public String getMinimumEngineVersion() {
    return minimumEngineVersion;
  }

  public void setMinimumEngineVersion(final String minimumEngineVersion) {
    this.minimumEngineVersion = minimumEngineVersion;
  }

  public String getParameterName() {
    return parameterName;
  }

  public void setParameterName(final String parameterName) {
    this.parameterName = parameterName;
  }

  public String getParameterValue() {
    return parameterValue;
  }

  public void setParameterValue(final String parameterValue) {
    this.parameterValue = parameterValue;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public EngineModeList getSupportedEngineModes() {
    return supportedEngineModes;
  }

  public void setSupportedEngineModes(final EngineModeList supportedEngineModes) {
    this.supportedEngineModes = supportedEngineModes;
  }

}
