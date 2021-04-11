/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBInstance extends EucalyptusData {

  private Integer allocatedStorage;

  private DBInstanceRoles associatedRoles;

  private Boolean autoMinorVersionUpgrade;

  private String availabilityZone;

  private Integer backupRetentionPeriod;

  private String cACertificateIdentifier;

  private String characterSetName;

  private Boolean copyTagsToSnapshot;

  private String dBClusterIdentifier;

  private String dBInstanceArn;

  private String dBInstanceClass;

  private String dBInstanceIdentifier;

  private String dBInstanceStatus;

  private String dBName;

  private DBParameterGroupStatusList dBParameterGroups;

  private DBSecurityGroupMembershipList dBSecurityGroups;

  private DBSubnetGroup dBSubnetGroup;

  private Integer dbInstancePort;

  private String dbiResourceId;

  private Boolean deletionProtection;

  private DomainMembershipList domainMemberships;

  private LogTypeList enabledCloudwatchLogsExports;

  private Endpoint endpoint;

  private String engine;

  private String engineVersion;

  private String enhancedMonitoringResourceArn;

  private Boolean iAMDatabaseAuthenticationEnabled;

  private java.util.Date instanceCreateTime;

  private Integer iops;

  private String kmsKeyId;

  private java.util.Date latestRestorableTime;

  private String licenseModel;

  private Endpoint listenerEndpoint;

  private String masterUsername;

  private Integer maxAllocatedStorage;

  private Integer monitoringInterval;

  private String monitoringRoleArn;

  private Boolean multiAZ;

  private OptionGroupMembershipList optionGroupMemberships;

  private PendingModifiedValues pendingModifiedValues;

  private Boolean performanceInsightsEnabled;

  private String performanceInsightsKMSKeyId;

  private Integer performanceInsightsRetentionPeriod;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private ProcessorFeatureList processorFeatures;

  private Integer promotionTier;

  private Boolean publiclyAccessible;

  private ReadReplicaDBClusterIdentifierList readReplicaDBClusterIdentifiers;

  private ReadReplicaDBInstanceIdentifierList readReplicaDBInstanceIdentifiers;

  private String readReplicaSourceDBInstanceIdentifier;

  private String secondaryAvailabilityZone;

  private DBInstanceStatusInfoList statusInfos;

  private Boolean storageEncrypted;

  private String storageType;

  private String tdeCredentialArn;

  private String timezone;

  private VpcSecurityGroupMembershipList vpcSecurityGroups;

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public DBInstanceRoles getAssociatedRoles() {
    return associatedRoles;
  }

  public void setAssociatedRoles(final DBInstanceRoles associatedRoles) {
    this.associatedRoles = associatedRoles;
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

  public String getCACertificateIdentifier() {
    return cACertificateIdentifier;
  }

  public void setCACertificateIdentifier(final String cACertificateIdentifier) {
    this.cACertificateIdentifier = cACertificateIdentifier;
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

  public String getDBInstanceArn() {
    return dBInstanceArn;
  }

  public void setDBInstanceArn(final String dBInstanceArn) {
    this.dBInstanceArn = dBInstanceArn;
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

  public String getDBInstanceStatus() {
    return dBInstanceStatus;
  }

  public void setDBInstanceStatus(final String dBInstanceStatus) {
    this.dBInstanceStatus = dBInstanceStatus;
  }

  public String getDBName() {
    return dBName;
  }

  public void setDBName(final String dBName) {
    this.dBName = dBName;
  }

  public DBParameterGroupStatusList getDBParameterGroups() {
    return dBParameterGroups;
  }

  public void setDBParameterGroups(final DBParameterGroupStatusList dBParameterGroups) {
    this.dBParameterGroups = dBParameterGroups;
  }

  public DBSecurityGroupMembershipList getDBSecurityGroups() {
    return dBSecurityGroups;
  }

  public void setDBSecurityGroups(final DBSecurityGroupMembershipList dBSecurityGroups) {
    this.dBSecurityGroups = dBSecurityGroups;
  }

  public DBSubnetGroup getDBSubnetGroup() {
    return dBSubnetGroup;
  }

  public void setDBSubnetGroup(final DBSubnetGroup dBSubnetGroup) {
    this.dBSubnetGroup = dBSubnetGroup;
  }

  public Integer getDbInstancePort() {
    return dbInstancePort;
  }

  public void setDbInstancePort(final Integer dbInstancePort) {
    this.dbInstancePort = dbInstancePort;
  }

  public String getDbiResourceId() {
    return dbiResourceId;
  }

  public void setDbiResourceId(final String dbiResourceId) {
    this.dbiResourceId = dbiResourceId;
  }

  public Boolean getDeletionProtection() {
    return deletionProtection;
  }

  public void setDeletionProtection(final Boolean deletionProtection) {
    this.deletionProtection = deletionProtection;
  }

  public DomainMembershipList getDomainMemberships() {
    return domainMemberships;
  }

  public void setDomainMemberships(final DomainMembershipList domainMemberships) {
    this.domainMemberships = domainMemberships;
  }

  public LogTypeList getEnabledCloudwatchLogsExports() {
    return enabledCloudwatchLogsExports;
  }

  public void setEnabledCloudwatchLogsExports(final LogTypeList enabledCloudwatchLogsExports) {
    this.enabledCloudwatchLogsExports = enabledCloudwatchLogsExports;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final Endpoint endpoint) {
    this.endpoint = endpoint;
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

  public String getEnhancedMonitoringResourceArn() {
    return enhancedMonitoringResourceArn;
  }

  public void setEnhancedMonitoringResourceArn(final String enhancedMonitoringResourceArn) {
    this.enhancedMonitoringResourceArn = enhancedMonitoringResourceArn;
  }

  public Boolean getIAMDatabaseAuthenticationEnabled() {
    return iAMDatabaseAuthenticationEnabled;
  }

  public void setIAMDatabaseAuthenticationEnabled(final Boolean iAMDatabaseAuthenticationEnabled) {
    this.iAMDatabaseAuthenticationEnabled = iAMDatabaseAuthenticationEnabled;
  }

  public java.util.Date getInstanceCreateTime() {
    return instanceCreateTime;
  }

  public void setInstanceCreateTime(final java.util.Date instanceCreateTime) {
    this.instanceCreateTime = instanceCreateTime;
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

  public java.util.Date getLatestRestorableTime() {
    return latestRestorableTime;
  }

  public void setLatestRestorableTime(final java.util.Date latestRestorableTime) {
    this.latestRestorableTime = latestRestorableTime;
  }

  public String getLicenseModel() {
    return licenseModel;
  }

  public void setLicenseModel(final String licenseModel) {
    this.licenseModel = licenseModel;
  }

  public Endpoint getListenerEndpoint() {
    return listenerEndpoint;
  }

  public void setListenerEndpoint(final Endpoint listenerEndpoint) {
    this.listenerEndpoint = listenerEndpoint;
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

  public OptionGroupMembershipList getOptionGroupMemberships() {
    return optionGroupMemberships;
  }

  public void setOptionGroupMemberships(final OptionGroupMembershipList optionGroupMemberships) {
    this.optionGroupMemberships = optionGroupMemberships;
  }

  public PendingModifiedValues getPendingModifiedValues() {
    return pendingModifiedValues;
  }

  public void setPendingModifiedValues(final PendingModifiedValues pendingModifiedValues) {
    this.pendingModifiedValues = pendingModifiedValues;
  }

  public Boolean getPerformanceInsightsEnabled() {
    return performanceInsightsEnabled;
  }

  public void setPerformanceInsightsEnabled(final Boolean performanceInsightsEnabled) {
    this.performanceInsightsEnabled = performanceInsightsEnabled;
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

  public ReadReplicaDBClusterIdentifierList getReadReplicaDBClusterIdentifiers() {
    return readReplicaDBClusterIdentifiers;
  }

  public void setReadReplicaDBClusterIdentifiers(final ReadReplicaDBClusterIdentifierList readReplicaDBClusterIdentifiers) {
    this.readReplicaDBClusterIdentifiers = readReplicaDBClusterIdentifiers;
  }

  public ReadReplicaDBInstanceIdentifierList getReadReplicaDBInstanceIdentifiers() {
    return readReplicaDBInstanceIdentifiers;
  }

  public void setReadReplicaDBInstanceIdentifiers(final ReadReplicaDBInstanceIdentifierList readReplicaDBInstanceIdentifiers) {
    this.readReplicaDBInstanceIdentifiers = readReplicaDBInstanceIdentifiers;
  }

  public String getReadReplicaSourceDBInstanceIdentifier() {
    return readReplicaSourceDBInstanceIdentifier;
  }

  public void setReadReplicaSourceDBInstanceIdentifier(final String readReplicaSourceDBInstanceIdentifier) {
    this.readReplicaSourceDBInstanceIdentifier = readReplicaSourceDBInstanceIdentifier;
  }

  public String getSecondaryAvailabilityZone() {
    return secondaryAvailabilityZone;
  }

  public void setSecondaryAvailabilityZone(final String secondaryAvailabilityZone) {
    this.secondaryAvailabilityZone = secondaryAvailabilityZone;
  }

  public DBInstanceStatusInfoList getStatusInfos() {
    return statusInfos;
  }

  public void setStatusInfos(final DBInstanceStatusInfoList statusInfos) {
    this.statusInfos = statusInfos;
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

  public String getTdeCredentialArn() {
    return tdeCredentialArn;
  }

  public void setTdeCredentialArn(final String tdeCredentialArn) {
    this.tdeCredentialArn = tdeCredentialArn;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(final String timezone) {
    this.timezone = timezone;
  }

  public VpcSecurityGroupMembershipList getVpcSecurityGroups() {
    return vpcSecurityGroups;
  }

  public void setVpcSecurityGroups(final VpcSecurityGroupMembershipList vpcSecurityGroups) {
    this.vpcSecurityGroups = vpcSecurityGroups;
  }

}
