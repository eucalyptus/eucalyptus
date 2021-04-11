/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBInstanceType extends RdsMessage {

  private Integer allocatedStorage;

  private Boolean allowMajorVersionUpgrade;

  private Boolean applyImmediately;

  private Boolean autoMinorVersionUpgrade;

  private Integer backupRetentionPeriod;

  private String cACertificateIdentifier;

  private Boolean certificateRotationRestart;

  private CloudwatchLogsExportConfiguration cloudwatchLogsExportConfiguration;

  private Boolean copyTagsToSnapshot;

  private String dBInstanceClass;

  @Nonnull
  private String dBInstanceIdentifier;

  private String dBParameterGroupName;

  private Integer dBPortNumber;

  private DBSecurityGroupNameList dBSecurityGroups;

  private String dBSubnetGroupName;

  private Boolean deletionProtection;

  private String domain;

  private String domainIAMRoleName;

  private Boolean enableIAMDatabaseAuthentication;

  private Boolean enablePerformanceInsights;

  private String engineVersion;

  private Integer iops;

  private String licenseModel;

  private String masterUserPassword;

  private Integer maxAllocatedStorage;

  private Integer monitoringInterval;

  private String monitoringRoleArn;

  private Boolean multiAZ;

  private String newDBInstanceIdentifier;

  private String optionGroupName;

  private String performanceInsightsKMSKeyId;

  private Integer performanceInsightsRetentionPeriod;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private ProcessorFeatureList processorFeatures;

  private Integer promotionTier;

  private Boolean publiclyAccessible;

  private String storageType;

  private String tdeCredentialArn;

  private String tdeCredentialPassword;

  private Boolean useDefaultProcessorFeatures;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

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

  public Boolean getAutoMinorVersionUpgrade() {
    return autoMinorVersionUpgrade;
  }

  public void setAutoMinorVersionUpgrade(final Boolean autoMinorVersionUpgrade) {
    this.autoMinorVersionUpgrade = autoMinorVersionUpgrade;
  }

  public Integer getBackupRetentionPeriod() {
    return backupRetentionPeriod;
  }

  public void setBackupRetentionPeriod(final Integer backupRetentionPeriod) {
    this.backupRetentionPeriod = backupRetentionPeriod;
  }

  public String getCACertificateIdentifier() {
    return cACertificateIdentifier;
  }

  public void setCACertificateIdentifier(final String cACertificateIdentifier) {
    this.cACertificateIdentifier = cACertificateIdentifier;
  }

  public Boolean getCertificateRotationRestart() {
    return certificateRotationRestart;
  }

  public void setCertificateRotationRestart(final Boolean certificateRotationRestart) {
    this.certificateRotationRestart = certificateRotationRestart;
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

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public Integer getDBPortNumber() {
    return dBPortNumber;
  }

  public void setDBPortNumber(final Integer dBPortNumber) {
    this.dBPortNumber = dBPortNumber;
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

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getDomainIAMRoleName() {
    return domainIAMRoleName;
  }

  public void setDomainIAMRoleName(final String domainIAMRoleName) {
    this.domainIAMRoleName = domainIAMRoleName;
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

  public Integer getMaxAllocatedStorage() {
    return maxAllocatedStorage;
  }

  public void setMaxAllocatedStorage(final Integer maxAllocatedStorage) {
    this.maxAllocatedStorage = maxAllocatedStorage;
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

  public String getNewDBInstanceIdentifier() {
    return newDBInstanceIdentifier;
  }

  public void setNewDBInstanceIdentifier(final String newDBInstanceIdentifier) {
    this.newDBInstanceIdentifier = newDBInstanceIdentifier;
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

  public Integer getPromotionTier() {
    return promotionTier;
  }

  public void setPromotionTier(final Integer promotionTier) {
    this.promotionTier = promotionTier;
  }

  public Boolean getPubliclyAccessible() {
    return publiclyAccessible;
  }

  public void setPubliclyAccessible(final Boolean publiclyAccessible) {
    this.publiclyAccessible = publiclyAccessible;
  }

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(final String storageType) {
    this.storageType = storageType;
  }

  public String getTdeCredentialArn() {
    return tdeCredentialArn;
  }

  public void setTdeCredentialArn(final String tdeCredentialArn) {
    this.tdeCredentialArn = tdeCredentialArn;
  }

  public String getTdeCredentialPassword() {
    return tdeCredentialPassword;
  }

  public void setTdeCredentialPassword(final String tdeCredentialPassword) {
    this.tdeCredentialPassword = tdeCredentialPassword;
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
