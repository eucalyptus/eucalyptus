/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBClusterType extends RdsMessage {

  private Boolean allowMajorVersionUpgrade;

  private Boolean applyImmediately;

  private Long backtrackWindow;

  private Integer backupRetentionPeriod;

  private CloudwatchLogsExportConfiguration cloudwatchLogsExportConfiguration;

  private Boolean copyTagsToSnapshot;

  @Nonnull
  private String dBClusterIdentifier;

  private String dBClusterParameterGroupName;

  private String dBInstanceParameterGroupName;

  private Boolean deletionProtection;

  private Boolean enableHttpEndpoint;

  private Boolean enableIAMDatabaseAuthentication;

  private String engineVersion;

  private String masterUserPassword;

  private String newDBClusterIdentifier;

  private String optionGroupName;

  private Integer port;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private ScalingConfiguration scalingConfiguration;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

  public Boolean getAllowMajorVersionUpgrade() {
    return allowMajorVersionUpgrade;
  }

  public void setAllowMajorVersionUpgrade(final Boolean allowMajorVersionUpgrade) {
    this.allowMajorVersionUpgrade = allowMajorVersionUpgrade;
  }

  public Boolean getApplyImmediately() {
    return applyImmediately;
  }

  public void setApplyImmediately(final Boolean applyImmediately) {
    this.applyImmediately = applyImmediately;
  }

  public Long getBacktrackWindow() {
    return backtrackWindow;
  }

  public void setBacktrackWindow(final Long backtrackWindow) {
    this.backtrackWindow = backtrackWindow;
  }

  public Integer getBackupRetentionPeriod() {
    return backupRetentionPeriod;
  }

  public void setBackupRetentionPeriod(final Integer backupRetentionPeriod) {
    this.backupRetentionPeriod = backupRetentionPeriod;
  }

  public CloudwatchLogsExportConfiguration getCloudwatchLogsExportConfiguration() {
    return cloudwatchLogsExportConfiguration;
  }

  public void setCloudwatchLogsExportConfiguration(final CloudwatchLogsExportConfiguration cloudwatchLogsExportConfiguration) {
    this.cloudwatchLogsExportConfiguration = cloudwatchLogsExportConfiguration;
  }

  public Boolean getCopyTagsToSnapshot() {
    return copyTagsToSnapshot;
  }

  public void setCopyTagsToSnapshot(final Boolean copyTagsToSnapshot) {
    this.copyTagsToSnapshot = copyTagsToSnapshot;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getDBClusterParameterGroupName() {
    return dBClusterParameterGroupName;
  }

  public void setDBClusterParameterGroupName(final String dBClusterParameterGroupName) {
    this.dBClusterParameterGroupName = dBClusterParameterGroupName;
  }

  public String getDBInstanceParameterGroupName() {
    return dBInstanceParameterGroupName;
  }

  public void setDBInstanceParameterGroupName(final String dBInstanceParameterGroupName) {
    this.dBInstanceParameterGroupName = dBInstanceParameterGroupName;
  }

  public Boolean getDeletionProtection() {
    return deletionProtection;
  }

  public void setDeletionProtection(final Boolean deletionProtection) {
    this.deletionProtection = deletionProtection;
  }

  public Boolean getEnableHttpEndpoint() {
    return enableHttpEndpoint;
  }

  public void setEnableHttpEndpoint(final Boolean enableHttpEndpoint) {
    this.enableHttpEndpoint = enableHttpEndpoint;
  }

  public Boolean getEnableIAMDatabaseAuthentication() {
    return enableIAMDatabaseAuthentication;
  }

  public void setEnableIAMDatabaseAuthentication(final Boolean enableIAMDatabaseAuthentication) {
    this.enableIAMDatabaseAuthentication = enableIAMDatabaseAuthentication;
  }

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
  }

  public String getMasterUserPassword() {
    return masterUserPassword;
  }

  public void setMasterUserPassword(final String masterUserPassword) {
    this.masterUserPassword = masterUserPassword;
  }

  public String getNewDBClusterIdentifier() {
    return newDBClusterIdentifier;
  }

  public void setNewDBClusterIdentifier(final String newDBClusterIdentifier) {
    this.newDBClusterIdentifier = newDBClusterIdentifier;
  }

  public String getOptionGroupName() {
    return optionGroupName;
  }

  public void setOptionGroupName(final String optionGroupName) {
    this.optionGroupName = optionGroupName;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public String getPreferredBackupWindow() {
    return preferredBackupWindow;
  }

  public void setPreferredBackupWindow(final String preferredBackupWindow) {
    this.preferredBackupWindow = preferredBackupWindow;
  }

  public String getPreferredMaintenanceWindow() {
    return preferredMaintenanceWindow;
  }

  public void setPreferredMaintenanceWindow(final String preferredMaintenanceWindow) {
    this.preferredMaintenanceWindow = preferredMaintenanceWindow;
  }

  public ScalingConfiguration getScalingConfiguration() {
    return scalingConfiguration;
  }

  public void setScalingConfiguration(final ScalingConfiguration scalingConfiguration) {
    this.scalingConfiguration = scalingConfiguration;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final VpcSecurityGroupIdList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

}
