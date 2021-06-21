/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRange;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;

public class DescribeDBEngineVersionsType extends RdsMessage {

  @FieldRegex(FieldRegexValue.STRING_128)
  private String dBParameterGroupFamily;

  private Boolean defaultOnly;

  @FieldRegex(FieldRegexValue.RDS_DB_ENGINE)
  private String engine;

  @FieldRegex(FieldRegexValue.RDS_DB_ENGINE_VERION)
  private String engineVersion;

  private FilterList filters;

  private Boolean includeAll;

  private Boolean listSupportedCharacterSets;

  private Boolean listSupportedTimezones;

  @FieldRegex(FieldRegexValue.STRING_128)
  private String marker;

  @FieldRange(min = 20, max = 100)
  private Integer maxRecords;

  public String getDBParameterGroupFamily() {
    return dBParameterGroupFamily;
  }

  public void setDBParameterGroupFamily(final String dBParameterGroupFamily) {
    this.dBParameterGroupFamily = dBParameterGroupFamily;
  }

  public Boolean getDefaultOnly() {
    return defaultOnly;
  }

  public void setDefaultOnly(final Boolean defaultOnly) {
    this.defaultOnly = defaultOnly;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(final String engine) {
    this.engine = engine;
  }

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
  }

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public Boolean getIncludeAll() {
    return includeAll;
  }

  public void setIncludeAll(final Boolean includeAll) {
    this.includeAll = includeAll;
  }

  public Boolean getListSupportedCharacterSets() {
    return listSupportedCharacterSets;
  }

  public void setListSupportedCharacterSets(final Boolean listSupportedCharacterSets) {
    this.listSupportedCharacterSets = listSupportedCharacterSets;
  }

  public Boolean getListSupportedTimezones() {
    return listSupportedTimezones;
  }

  public void setListSupportedTimezones(final Boolean listSupportedTimezones) {
    this.listSupportedTimezones = listSupportedTimezones;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(final Integer maxRecords) {
    this.maxRecords = maxRecords;
  }

}
