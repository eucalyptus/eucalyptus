/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OptionConfiguration extends EucalyptusData {

  private DBSecurityGroupNameList dBSecurityGroupMemberships;

  @Nonnull
  private String optionName;

  private OptionSettingsList optionSettings;

  private String optionVersion;

  private Integer port;

  private VpcSecurityGroupIdList vpcSecurityGroupMemberships;

  public DBSecurityGroupNameList getDBSecurityGroupMemberships() {
    return dBSecurityGroupMemberships;
  }

  public void setDBSecurityGroupMemberships(final DBSecurityGroupNameList dBSecurityGroupMemberships) {
    this.dBSecurityGroupMemberships = dBSecurityGroupMemberships;
  }

  public String getOptionName() {
    return optionName;
  }

  public void setOptionName(final String optionName) {
    this.optionName = optionName;
  }

  public OptionSettingsList getOptionSettings() {
    return optionSettings;
  }

  public void setOptionSettings(final OptionSettingsList optionSettings) {
    this.optionSettings = optionSettings;
  }

  public String getOptionVersion() {
    return optionVersion;
  }

  public void setOptionVersion(final String optionVersion) {
    this.optionVersion = optionVersion;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupMemberships() {
    return vpcSecurityGroupMemberships;
  }

  public void setVpcSecurityGroupMemberships(final VpcSecurityGroupIdList vpcSecurityGroupMemberships) {
    this.vpcSecurityGroupMemberships = vpcSecurityGroupMemberships;
  }

}
