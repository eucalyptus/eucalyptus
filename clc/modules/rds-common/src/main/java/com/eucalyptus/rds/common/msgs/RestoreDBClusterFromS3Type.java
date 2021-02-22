/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class RestoreDBClusterFromS3Type extends RdsMessage {

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

  private Boolean enableIAMDatabaseAuthentication;

  @Nonnull
  private String engine;

  private String engineVersion;

  private String kmsKeyId;

  @Nonnull
  private String masterUserPassword;

  @Nonnull
  private String masterUsername;

  private String optionGroupName;

  private Integer port;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  @Nonnull
  private String s3BucketName;

  @Nonnull
  private String s3IngestionRoleArn;

  private String s3Prefix;

  @Nonnull
  private String sourceEngine;

  @Nonnull
  private String sourceEngineVersion;

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

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
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

  public String getS3BucketName() {
    return s3BucketName;
  }

  public void setS3BucketName(final String s3BucketName) {
    this.s3BucketName = s3BucketName;
  }

  public String getS3IngestionRoleArn() {
    return s3IngestionRoleArn;
  }

  public void setS3IngestionRoleArn(final String s3IngestionRoleArn) {
    this.s3IngestionRoleArn = s3IngestionRoleArn;
  }

  public String getS3Prefix() {
    return s3Prefix;
  }

  public void setS3Prefix(final String s3Prefix) {
    this.s3Prefix = s3Prefix;
  }

  public String getSourceEngine() {
    return sourceEngine;
  }

  public void setSourceEngine(final String sourceEngine) {
    this.sourceEngine = sourceEngine;
  }

  public String getSourceEngineVersion() {
    return sourceEngineVersion;
  }

  public void setSourceEngineVersion(final String sourceEngineVersion) {
    this.sourceEngineVersion = sourceEngineVersion;
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
