/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OptionGroupOption extends EucalyptusData {

  private Integer defaultPort;

  private String description;

  private String engineName;

  private String majorEngineVersion;

  private String minimumRequiredMinorEngineVersion;

  private String name;

  private OptionGroupOptionSettingsList optionGroupOptionSettings;

  private OptionGroupOptionVersionsList optionGroupOptionVersions;

  private OptionsConflictsWith optionsConflictsWith;

  private OptionsDependedOn optionsDependedOn;

  private Boolean permanent;

  private Boolean persistent;

  private Boolean portRequired;

  private Boolean requiresAutoMinorEngineVersionUpgrade;

  private Boolean supportsOptionVersionDowngrade;

  private Boolean vpcOnly;

  public Integer getDefaultPort() {
    return defaultPort;
  }

  public void setDefaultPort(final Integer defaultPort) {
    this.defaultPort = defaultPort;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getEngineName() {
    return engineName;
  }

  public void setEngineName(final String engineName) {
    this.engineName = engineName;
  }

  public String getMajorEngineVersion() {
    return majorEngineVersion;
  }

  public void setMajorEngineVersion(final String majorEngineVersion) {
    this.majorEngineVersion = majorEngineVersion;
  }

  public String getMinimumRequiredMinorEngineVersion() {
    return minimumRequiredMinorEngineVersion;
  }

  public void setMinimumRequiredMinorEngineVersion(final String minimumRequiredMinorEngineVersion) {
    this.minimumRequiredMinorEngineVersion = minimumRequiredMinorEngineVersion;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public OptionGroupOptionSettingsList getOptionGroupOptionSettings() {
    return optionGroupOptionSettings;
  }

  public void setOptionGroupOptionSettings(final OptionGroupOptionSettingsList optionGroupOptionSettings) {
    this.optionGroupOptionSettings = optionGroupOptionSettings;
  }

  public OptionGroupOptionVersionsList getOptionGroupOptionVersions() {
    return optionGroupOptionVersions;
  }

  public void setOptionGroupOptionVersions(final OptionGroupOptionVersionsList optionGroupOptionVersions) {
    this.optionGroupOptionVersions = optionGroupOptionVersions;
  }

  public OptionsConflictsWith getOptionsConflictsWith() {
    return optionsConflictsWith;
  }

  public void setOptionsConflictsWith(final OptionsConflictsWith optionsConflictsWith) {
    this.optionsConflictsWith = optionsConflictsWith;
  }

  public OptionsDependedOn getOptionsDependedOn() {
    return optionsDependedOn;
  }

  public void setOptionsDependedOn(final OptionsDependedOn optionsDependedOn) {
    this.optionsDependedOn = optionsDependedOn;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public void setPermanent(final Boolean permanent) {
    this.permanent = permanent;
  }

  public Boolean getPersistent() {
    return persistent;
  }

  public void setPersistent(final Boolean persistent) {
    this.persistent = persistent;
  }

  public Boolean getPortRequired() {
    return portRequired;
  }

  public void setPortRequired(final Boolean portRequired) {
    this.portRequired = portRequired;
  }

  public Boolean getRequiresAutoMinorEngineVersionUpgrade() {
    return requiresAutoMinorEngineVersionUpgrade;
  }

  public void setRequiresAutoMinorEngineVersionUpgrade(final Boolean requiresAutoMinorEngineVersionUpgrade) {
    this.requiresAutoMinorEngineVersionUpgrade = requiresAutoMinorEngineVersionUpgrade;
  }

  public Boolean getSupportsOptionVersionDowngrade() {
    return supportsOptionVersionDowngrade;
  }

  public void setSupportsOptionVersionDowngrade(final Boolean supportsOptionVersionDowngrade) {
    this.supportsOptionVersionDowngrade = supportsOptionVersionDowngrade;
  }

  public Boolean getVpcOnly() {
    return vpcOnly;
  }

  public void setVpcOnly(final Boolean vpcOnly) {
    this.vpcOnly = vpcOnly;
  }

}
