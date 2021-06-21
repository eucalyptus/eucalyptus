/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRange;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import javax.annotation.Nonnull;


public class CreateDBInstanceType extends RdsMessage {

  @FieldRange(min=1, max=65536)
  private Integer allocatedStorage;

  private Boolean autoMinorVersionUpgrade;

  @FieldRegex(FieldRegexValue.STRING_128)
  private String availabilityZone;

  @FieldRange(min=0, max=35)
  private Integer backupRetentionPeriod;

  private String characterSetName;

  private Boolean copyTagsToSnapshot;

  @FieldRegex(FieldRegexValue.RDS_DB_CLUSTER_ID)
  private String dBClusterIdentifier;

  @Nonnull
  @FieldRegex(FieldRegexValue.STRING_128)
  private String dBInstanceClass;

  @Nonnull
  @FieldRegex(FieldRegexValue.RDS_DB_INSTANCE_ID)
  private String dBInstanceIdentifier;

  @FieldRegex(FieldRegexValue.STRING_64)
  private String dBName;

  @FieldRegex(FieldRegexValue.RDS_DB_PARAMETER_GROUP_NAME)
  private String dBParameterGroupName;

  private DBSecurityGroupNameList dBSecurityGroups;

  @FieldRegex(FieldRegexValue.RDS_DB_SUBNET_GROUP_NAME)
  private String dBSubnetGroupName;

  private Boolean deletionProtection;

  @FieldRegex(FieldRegexValue.STRING_256)
  private String domain;

  @FieldRegex(FieldRegexValue.STRING_256)
  private String domainIAMRoleName;

  private LogTypeList enableCloudwatchLogsExports;

  private Boolean enableIAMDatabaseAuthentication;

  private Boolean enablePerformanceInsights;

  @Nonnull
  @FieldRegex(FieldRegexValue.RDS_DB_ENGINE)
  private String engine;

  @FieldRegex(FieldRegexValue.RDS_DB_ENGINE_VERION)
  private String engineVersion;

  @FieldRange(min=100, max=1_000_000)
  private Integer iops;

  @FieldRegex(FieldRegexValue.KMS_NAME_OR_ARN)
  private String kmsKeyId;

  @FieldRegex(FieldRegexValue.ENUM_LICENSEMODEL)
  private String licenseModel;

  @FieldRegex(FieldRegexValue.RDS_DB_MASTERPASSWORD)
  private String masterUserPassword;

  @FieldRegex(FieldRegexValue.RDS_DB_MASTERUSERNAME)
  private String masterUsername;

  @FieldRange(min=0, max=1_000_000)
  private Integer maxAllocatedStorage;

  @FieldRange(max=60)
  private Integer monitoringInterval;

  @FieldRegex(FieldRegexValue.IAM_NAME_OR_ARN)
  private String monitoringRoleArn;

  private Boolean multiAZ;

  @FieldRegex(FieldRegexValue.RDS_DB_OPTION_GROUP_NAME)
  private String optionGroupName;

  @FieldRegex(FieldRegexValue.KMS_NAME_OR_ARN)
  private String performanceInsightsKMSKeyId;

  @FieldRange(max=1000)
  private Integer performanceInsightsRetentionPeriod;

  @FieldRange(min=1150, max=65535)
  private Integer port;

  @FieldRegex(FieldRegexValue.STRING_64)
  private String preferredBackupWindow;

  @FieldRegex(FieldRegexValue.STRING_64)
  private String preferredMaintenanceWindow;

  private ProcessorFeatureList processorFeatures;

  @FieldRange(min=0, max=15)
  private Integer promotionTier;

  private Boolean publiclyAccessible;

  private Boolean storageEncrypted;

  @FieldRegex(FieldRegexValue.ENUM_STORAGETYPE)
  private String storageType;

  private TagList tags;

  @FieldRegex(FieldRegexValue.KMS_NAME_OR_ARN)
  private String tdeCredentialArn;

  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private String tdeCredentialPassword;

  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private String timezone;

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

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(final String timezone) {
    this.timezone = timezone;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final VpcSecurityGroupIdList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

}
