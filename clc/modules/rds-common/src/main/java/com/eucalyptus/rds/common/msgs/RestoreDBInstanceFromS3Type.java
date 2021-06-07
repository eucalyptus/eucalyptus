/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class RestoreDBInstanceFromS3Type extends RdsMessage {

  private Integer allocatedStorage;

  private Boolean autoMinorVersionUpgrade;

  private String availabilityZone;

  private Integer backupRetentionPeriod;

  private Boolean copyTagsToSnapshot;

  @Nonnull
  private String dBInstanceClass;

  @Nonnull
  private String dBInstanceIdentifier;

  private String dBName;

  private String dBParameterGroupName;

  private DBSecurityGroupNameList dBSecurityGroups;

  private String dBSubnetGroupName;

  private Boolean deletionProtection;

  private LogTypeList enableCloudwatchLogsExports;

  private Boolean enableIAMDatabaseAuthentication;

  private Boolean enablePerformanceInsights;

  @Nonnull
  private String engine;

  private String engineVersion;

  private Integer iops;

  private String kmsKeyId;

  private String licenseModel;

  private String masterUserPassword;

  private String masterUsername;

  private Integer monitoringInterval;

  private String monitoringRoleArn;

  private Boolean multiAZ;

  private String optionGroupName;

  private String performanceInsightsKMSKeyId;

  private Integer performanceInsightsRetentionPeriod;

  private Integer port;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private ProcessorFeatureList processorFeatures;

  private Boolean publiclyAccessible;

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

  private String storageType;

  private TagList tags;

  private Boolean useDefaultProcessorFeatures;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public Boolean getAutoMinorVersionUpgrade() {
    return autoMinorVersionUpgrade;
  }

  public void setAutoMinorVersionUpgrade(final Boolean autoMinorVersionUpgrade) {
    this.autoMinorVersionUpgrade = autoMinorVersionUpgrade;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(final String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public Integer getBackupRetentionPeriod() {
    return backupRetentionPeriod;
  }

  public void setBackupRetentionPeriod(final Integer backupRetentionPeriod) {
    this.backupRetentionPeriod = backupRetentionPeriod;
  }

  public Boolean getCopyTagsToSnapshot() {
    return copyTagsToSnapshot;
  }

  public void setCopyTagsToSnapshot(final Boolean copyTagsToSnapshot) {
    this.copyTagsToSnapshot = copyTagsToSnapshot;
  }

  public String getDBInstanceClass() {
    return dBInstanceClass;
  }

  public void setDBInstanceClass(final String dBInstanceClass) {
    this.dBInstanceClass = dBInstanceClass;
  }

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public String getDBName() {
    return dBName;
  }

  public void setDBName(final String dBName) {
    this.dBName = dBName;
  }

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public DBSecurityGroupNameList getDBSecurityGroups() {
    return dBSecurityGroups;
  }

  public void setDBSecurityGroups(final DBSecurityGroupNameList dBSecurityGroups) {
    this.dBSecurityGroups = dBSecurityGroups;
  }

  public String getDBSubnetGroupName() {
    return dBSubnetGroupName;
  }

  public void setDBSubnetGroupName(final String dBSubnetGroupName) {
    this.dBSubnetGroupName = dBSubnetGroupName;
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

  public Boolean getEnablePerformanceInsights() {
    return enablePerformanceInsights;
  }

  public void setEnablePerformanceInsights(final Boolean enablePerformanceInsights) {
    this.enablePerformanceInsights = enablePerformanceInsights;
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

  public Integer getIops() {
    return iops;
  }

  public void setIops(final Integer iops) {
    this.iops = iops;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getLicenseModel() {
    return licenseModel;
  }

  public void setLicenseModel(final String licenseModel) {
    this.licenseModel = licenseModel;
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

  public Integer getMonitoringInterval() {
    return monitoringInterval;
  }

  public void setMonitoringInterval(final Integer monitoringInterval) {
    this.monitoringInterval = monitoringInterval;
  }

  public String getMonitoringRoleArn() {
    return monitoringRoleArn;
  }

  public void setMonitoringRoleArn(final String monitoringRoleArn) {
    this.monitoringRoleArn = monitoringRoleArn;
  }

  public Boolean getMultiAZ() {
    return multiAZ;
  }

  public void setMultiAZ(final Boolean multiAZ) {
    this.multiAZ = multiAZ;
  }

  public String getOptionGroupName() {
    return optionGroupName;
  }

  public void setOptionGroupName(final String optionGroupName) {
    this.optionGroupName = optionGroupName;
  }

  public String getPerformanceInsightsKMSKeyId() {
    return performanceInsightsKMSKeyId;
  }

  public void setPerformanceInsightsKMSKeyId(final String performanceInsightsKMSKeyId) {
    this.performanceInsightsKMSKeyId = performanceInsightsKMSKeyId;
  }

  public Integer getPerformanceInsightsRetentionPeriod() {
    return performanceInsightsRetentionPeriod;
  }

  public void setPerformanceInsightsRetentionPeriod(final Integer performanceInsightsRetentionPeriod) {
    this.performanceInsightsRetentionPeriod = performanceInsightsRetentionPeriod;
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

  public ProcessorFeatureList getProcessorFeatures() {
    return processorFeatures;
  }

  public void setProcessorFeatures(final ProcessorFeatureList processorFeatures) {
    this.processorFeatures = processorFeatures;
  }

  public Boolean getPubliclyAccessible() {
    return publiclyAccessible;
  }

  public void setPubliclyAccessible(final Boolean publiclyAccessible) {
    this.publiclyAccessible = publiclyAccessible;
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

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(final String storageType) {
    this.storageType = storageType;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public Boolean getUseDefaultProcessorFeatures() {
    return useDefaultProcessorFeatures;
  }

  public void setUseDefaultProcessorFeatures(final Boolean useDefaultProcessorFeatures) {
    this.useDefaultProcessorFeatures = useDefaultProcessorFeatures;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final VpcSecurityGroupIdList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

}
