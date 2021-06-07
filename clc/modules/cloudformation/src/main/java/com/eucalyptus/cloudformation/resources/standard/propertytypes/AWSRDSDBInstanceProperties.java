/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class AWSRDSDBInstanceProperties implements ResourceProperties {

  @Property
  private String allocatedStorage;

  @Property
  private Boolean allowMajorVersionUpgrade;

  @Property
  private Boolean autoMinorVersionUpgrade;

  @Property
  private String availabilityZone;

  @Property
  private Integer backupRetentionPeriod;

  @Property
  private Boolean copyTagsToSnapshot;

  @Required
  @Property(name="DBInstanceClass")
  private String dbInstanceClass;

  @Property(name="DBInstanceIdentifier")
  private String dbInstanceIdentifier;

  @Property(name="DBName")
  private String dbName;

  @Property(name="DBParameterGroupName")
  private String dbParameterGroupName;

  @Property(name="DBSnapshotIdentifier")
  private String dbSnapshotIdentifier;

  @Property(name="DBSubnetGroupName")
  private String dbSubnetGroupName;

  @Property
  private Boolean deleteAutomatedBackups;

  @Property
  private Boolean deletionProtection;

  @Property
  private String domain;

  @Property
  private String domainIAMRoleName;

  @Property
  private Boolean enableIAMDatabaseAuthentication;

  @Property
  private Boolean enablePerformanceInsights;

  @Property
  private String engine;

  @Property
  private String engineVersion;

  @Property
  private Integer iops;

  @Property
  private String kmsKeyId;

  @Property
  private String licenseModel;

  @Property
  private String masterUsername;

  @Property
  private String masterUserPassword;

  @Property
  private Integer maxAllocatedStorage;

  @Property
  private Integer monitoringInterval;

  @Property
  private String monitoringRoleArn;

  @Property
  private Boolean multiAZ;

  @Property
  private String optionGroupName;

  @Property
  private String performanceInsightsKMSKeyId;

  @Property
  private Integer performanceInsightsRetentionPeriod;

  @Property
  private String port;

  @Property
  private String preferredBackupWindow;

  @Property
  private String preferredMaintenanceWindow;

  @Property
  private Integer promotionTier;

  @Property
  private Boolean publiclyAccessible;

  @Property
  private String sourceDBInstanceIdentifier;

  @Property
  private String sourceRegion;

  @Property
  private Boolean storageEncrypted;

  @Property
  private String storageType;

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  @Property
  private String timezone;

  @Property
  private Boolean useDefaultProcessorFeatures;

  @Property(name="VPCSecurityGroups")
  private ArrayList<String> vpcSecurityGroups = Lists.newArrayList( );

  public String getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final String allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public Boolean getAllowMajorVersionUpgrade() {
    return allowMajorVersionUpgrade;
  }

  public void setAllowMajorVersionUpgrade(final Boolean allowMajorVersionUpgrade) {
    this.allowMajorVersionUpgrade = allowMajorVersionUpgrade;
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

  public String getDbInstanceClass() {
    return dbInstanceClass;
  }

  public void setDbInstanceClass(final String dbInstanceClass) {
    this.dbInstanceClass = dbInstanceClass;
  }

  public String getDbInstanceIdentifier() {
    return dbInstanceIdentifier;
  }

  public void setDbInstanceIdentifier(final String dbInstanceIdentifier) {
    this.dbInstanceIdentifier = dbInstanceIdentifier;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(final String dbName) {
    this.dbName = dbName;
  }

  public String getDbParameterGroupName() {
    return dbParameterGroupName;
  }

  public void setDbParameterGroupName(final String dbParameterGroupName) {
    this.dbParameterGroupName = dbParameterGroupName;
  }

  public String getDbSnapshotIdentifier() {
    return dbSnapshotIdentifier;
  }

  public void setDbSnapshotIdentifier(final String dbSnapshotIdentifier) {
    this.dbSnapshotIdentifier = dbSnapshotIdentifier;
  }

  public String getDbSubnetGroupName() {
    return dbSubnetGroupName;
  }

  public void setDbSubnetGroupName(final String dbSubnetGroupName) {
    this.dbSubnetGroupName = dbSubnetGroupName;
  }

  public Boolean getDeleteAutomatedBackups() {
    return deleteAutomatedBackups;
  }

  public void setDeleteAutomatedBackups(final Boolean deleteAutomatedBackups) {
    this.deleteAutomatedBackups = deleteAutomatedBackups;
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

  public String getMasterUsername() {
    return masterUsername;
  }

  public void setMasterUsername(final String masterUsername) {
    this.masterUsername = masterUsername;
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

  public String getPort() {
    return port;
  }

  public void setPort(final String port) {
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

  public String getSourceDBInstanceIdentifier() {
    return sourceDBInstanceIdentifier;
  }

  public void setSourceDBInstanceIdentifier(final String sourceDBInstanceIdentifier) {
    this.sourceDBInstanceIdentifier = sourceDBInstanceIdentifier;
  }

  public String getSourceRegion() {
    return sourceRegion;
  }

  public void setSourceRegion(final String sourceRegion) {
    this.sourceRegion = sourceRegion;
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

  public ArrayList<CloudFormationResourceTag> getTags() {
    return tags;
  }

  public void setTags(final ArrayList<CloudFormationResourceTag> tags) {
    this.tags = tags;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(final String timezone) {
    this.timezone = timezone;
  }

  public Boolean getUseDefaultProcessorFeatures() {
    return useDefaultProcessorFeatures;
  }

  public void setUseDefaultProcessorFeatures(final Boolean useDefaultProcessorFeatures) {
    this.useDefaultProcessorFeatures = useDefaultProcessorFeatures;
  }

  public ArrayList<String> getVpcSecurityGroups() {
    return vpcSecurityGroups;
  }

  public void setVpcSecurityGroups(final ArrayList<String> vpcSecurityGroups) {
    this.vpcSecurityGroups = vpcSecurityGroups;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("allocatedStorage", allocatedStorage)
        .add("allowMajorVersionUpgrade", allowMajorVersionUpgrade)
        .add("autoMinorVersionUpgrade", autoMinorVersionUpgrade)
        .add("availabilityZone", availabilityZone)
        .add("backupRetentionPeriod", backupRetentionPeriod)
        .add("copyTagsToSnapshot", copyTagsToSnapshot)
        .add("dbInstanceClass", dbInstanceClass)
        .add("dbInstanceIdentifier", dbInstanceIdentifier)
        .add("dbName", dbName)
        .add("dbParameterGroupName", dbParameterGroupName)
        .add("dbSnapshotIdentifier", dbSnapshotIdentifier)
        .add("dbSubnetGroupName", dbSubnetGroupName)
        .add("deleteAutomatedBackups", deleteAutomatedBackups)
        .add("deletionProtection", deletionProtection)
        .add("domain", domain)
        .add("domainIAMRoleName", domainIAMRoleName)
        .add("enableIAMDatabaseAuthentication", enableIAMDatabaseAuthentication)
        .add("enablePerformanceInsights", enablePerformanceInsights)
        .add("engine", engine)
        .add("engineVersion", engineVersion)
        .add("iops", iops)
        .add("kmsKeyId", kmsKeyId)
        .add("licenseModel", licenseModel)
        .add("masterUsername", masterUsername)
        .add("maxAllocatedStorage", maxAllocatedStorage)
        .add("monitoringInterval", monitoringInterval)
        .add("monitoringRoleArn", monitoringRoleArn)
        .add("multiAZ", multiAZ)
        .add("optionGroupName", optionGroupName)
        .add("performanceInsightsKMSKeyId", performanceInsightsKMSKeyId)
        .add("performanceInsightsRetentionPeriod", performanceInsightsRetentionPeriod)
        .add("port", port)
        .add("preferredBackupWindow", preferredBackupWindow)
        .add("preferredMaintenanceWindow", preferredMaintenanceWindow)
        .add("promotionTier", promotionTier)
        .add("publiclyAccessible", publiclyAccessible)
        .add("sourceDBInstanceIdentifier", sourceDBInstanceIdentifier)
        .add("sourceRegion", sourceRegion)
        .add("storageEncrypted", storageEncrypted)
        .add("storageType", storageType)
        .add("tags", tags)
        .add("timezone", timezone)
        .add("useDefaultProcessorFeatures", useDefaultProcessorFeatures)
        .add("vpcSecurityGroups", vpcSecurityGroups)
        .toString();
  }
}
