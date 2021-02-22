/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Option extends EucalyptusData {

  private DBSecurityGroupMembershipList dBSecurityGroupMemberships;

  private String optionDescription;

  private String optionName;

  private OptionSettingConfigurationList optionSettings;

  private String optionVersion;

  private Boolean permanent;

  private Boolean persistent;

  private Integer port;

  private VpcSecurityGroupMembershipList vpcSecurityGroupMemberships;

  public DBSecurityGroupMembershipList getDBSecurityGroupMemberships() {
    return dBSecurityGroupMemberships;
  }

  public void setDBSecurityGroupMemberships(final DBSecurityGroupMembershipList dBSecurityGroupMemberships) {
    this.dBSecurityGroupMemberships = dBSecurityGroupMemberships;
  }

  public String getOptionDescription() {
    return optionDescription;
  }

  public void setOptionDescription(final String optionDescription) {
    this.optionDescription = optionDescription;
  }

  public String getOptionName() {
    return optionName;
  }

  public void setOptionName(final String optionName) {
    this.optionName = optionName;
  }

  public OptionSettingConfigurationList getOptionSettings() {
    return optionSettings;
  }

  public void setOptionSettings(final OptionSettingConfigurationList optionSettings) {
    this.optionSettings = optionSettings;
  }

  public String getOptionVersion() {
    return optionVersion;
  }

  public void setOptionVersion(final String optionVersion) {
    this.optionVersion = optionVersion;
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

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public VpcSecurityGroupMembershipList getVpcSecurityGroupMemberships() {
    return vpcSecurityGroupMemberships;
  }

  public void setVpcSecurityGroupMemberships(final VpcSecurityGroupMembershipList vpcSecurityGroupMemberships) {
    this.vpcSecurityGroupMemberships = vpcSecurityGroupMemberships;
  }

}
