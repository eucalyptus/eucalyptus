/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBClusterType extends RdsMessage {

  private AvailabilityZones availabilityZones;

  private Long backtrackWindow;

  private Integer backupRetentionPeriod;

  private String characterSetName;

  private Boolean copyTagsToSnapshot;

  @Nonnull
  private String dBClusterIdentifier;

  private String dBClusterParameterGroupName;

  private String dBSubnetGroupName;

  private String databaseName;

  private Boolean deletionProtection;

  private LogTypeList enableCloudwatchLogsExports;

  private Boolean enableHttpEndpoint;

  private Boolean enableIAMDatabaseAuthentication;

  @Nonnull
  private String engine;

  private String engineMode;

  private String engineVersion;

  private String globalClusterIdentifier;

  private String kmsKeyId;

  private String masterUserPassword;

  private String masterUsername;

  private String optionGroupName;

  private Integer port;

  private String preSignedUrl;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private String replicationSourceIdentifier;

  private ScalingConfiguration scalingConfiguration;

  private Boolean storageEncrypted;

  private TagList tags;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

  public AvailabilityZones getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(final AvailabilityZones availabilityZones) {
    this.availabilityZones = availabilityZones;
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

  public String getCharacterSetName() {
    return characterSetName;
  }

  public void setCharacterSetName(final String characterSetName) {
    this.characterSetName = characterSetName;
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

  public String getDBSubnetGroupName() {
    return dBSubnetGroupName;
  }

  public void setDBSubnetGroupName(final String dBSubnetGroupName) {
    this.dBSubnetGroupName = dBSubnetGroupName;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(final String databaseName) {
    this.databaseName = databaseName;
  }

  public Boolean getDeletionProtection() {
    return deletionProtection;
  }

  public void setDeletionProtection(final Boolean deletionProtection) {
    this.deletionProtection = deletionProtection;
  }

  public LogTypeList getEnableCloudwatchLogsExports() {
    return enableCloudwatchLogsExports;
  }

  public void setEnableCloudwatchLogsExports(final LogTypeList enableCloudwatchLogsExports) {
    this.enableCloudwatchLogsExports = enableCloudwatchLogsExports;
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

  public String getEngine() {
    return engine;
  }

  public void setEngine(final String engine) {
    this.engine = engine;
  }

  public String getEngineMode() {
    return engineMode;
  }

  public void setEngineMode(final String engineMode) {
    this.engineMode = engineMode;
  }

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
  }

  public String getGlobalClusterIdentifier() {
    return globalClusterIdentifier;
  }

  public void setGlobalClusterIdentifier(final String globalClusterIdentifier) {
    this.globalClusterIdentifier = globalClusterIdentifier;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getMasterUserPassword() {
    return masterUserPassword;
  }

  public void setMasterUserPassword(final String masterUserPassword) {
    this.masterUserPassword = masterUserPassword;
  }

  public String getMasterUsername() {
    return masterUsername;
  }

  public void setMasterUsername(final String masterUsername) {
    this.masterUsername = masterUsername;
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

  public String getPreSignedUrl() {
    return preSignedUrl;
  }

  public void setPreSignedUrl(final String preSignedUrl) {
    this.preSignedUrl = preSignedUrl;
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

  public String getReplicationSourceIdentifier() {
    return replicationSourceIdentifier;
  }

  public void setReplicationSourceIdentifier(final String replicationSourceIdentifier) {
    this.replicationSourceIdentifier = replicationSourceIdentifier;
  }

  public ScalingConfiguration getScalingConfiguration() {
    return scalingConfiguration;
  }

  public void setScalingConfiguration(final ScalingConfiguration scalingConfiguration) {
    this.scalingConfiguration = scalingConfiguration;
  }

  public Boolean getStorageEncrypted() {
    return storageEncrypted;
  }

  public void setStorageEncrypted(final Boolean storageEncrypted) {
    this.storageEncrypted = storageEncrypted;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final VpcSecurityGroupIdList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

}
