/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBCluster extends EucalyptusData {

  private String activityStreamKinesisStreamName;

  private String activityStreamKmsKeyId;

  @FieldRegex(FieldRegexValue.ENUM_ACTIVITYSTREAMMODE)
  private String activityStreamMode;

  @FieldRegex(FieldRegexValue.ENUM_ACTIVITYSTREAMSTATUS)
  private String activityStreamStatus;

  private Integer allocatedStorage;

  private DBClusterRoles associatedRoles;

  private AvailabilityZones availabilityZones;

  private Long backtrackConsumedChangeRecords;

  private Long backtrackWindow;

  private Integer backupRetentionPeriod;

  private Integer capacity;

  private String characterSetName;

  private String cloneGroupId;

  private java.util.Date clusterCreateTime;

  private Boolean copyTagsToSnapshot;

  private Boolean crossAccountClone;

  private StringList customEndpoints;

  private String dBClusterArn;

  private String dBClusterIdentifier;

  private DBClusterMemberList dBClusterMembers;

  private DBClusterOptionGroupMemberships dBClusterOptionGroupMemberships;

  private String dBClusterParameterGroup;

  private String dBSubnetGroup;

  private String databaseName;

  private String dbClusterResourceId;

  private Boolean deletionProtection;

  private java.util.Date earliestBacktrackTime;

  private java.util.Date earliestRestorableTime;

  private LogTypeList enabledCloudwatchLogsExports;

  private String endpoint;

  private String engine;

  private String engineMode;

  private String engineVersion;

  private String hostedZoneId;

  private Boolean httpEndpointEnabled;

  private Boolean iAMDatabaseAuthenticationEnabled;

  private String kmsKeyId;

  private java.util.Date latestRestorableTime;

  private String masterUsername;

  private Boolean multiAZ;

  private String percentProgress;

  private Integer port;

  private String preferredBackupWindow;

  private String preferredMaintenanceWindow;

  private ReadReplicaIdentifierList readReplicaIdentifiers;

  private String readerEndpoint;

  private String replicationSourceIdentifier;

  private ScalingConfigurationInfo scalingConfigurationInfo;

  private String status;

  private Boolean storageEncrypted;

  private VpcSecurityGroupMembershipList vpcSecurityGroups;

  public String getActivityStreamKinesisStreamName() {
    return activityStreamKinesisStreamName;
  }

  public void setActivityStreamKinesisStreamName(final String activityStreamKinesisStreamName) {
    this.activityStreamKinesisStreamName = activityStreamKinesisStreamName;
  }

  public String getActivityStreamKmsKeyId() {
    return activityStreamKmsKeyId;
  }

  public void setActivityStreamKmsKeyId(final String activityStreamKmsKeyId) {
    this.activityStreamKmsKeyId = activityStreamKmsKeyId;
  }

  public String getActivityStreamMode() {
    return activityStreamMode;
  }

  public void setActivityStreamMode(final String activityStreamMode) {
    this.activityStreamMode = activityStreamMode;
  }

  public String getActivityStreamStatus() {
    return activityStreamStatus;
  }

  public void setActivityStreamStatus(final String activityStreamStatus) {
    this.activityStreamStatus = activityStreamStatus;
  }

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public DBClusterRoles getAssociatedRoles() {
    return associatedRoles;
  }

  public void setAssociatedRoles(final DBClusterRoles associatedRoles) {
    this.associatedRoles = associatedRoles;
  }

  public AvailabilityZones getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(final AvailabilityZones availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  public Long getBacktrackConsumedChangeRecords() {
    return backtrackConsumedChangeRecords;
  }

  public void setBacktrackConsumedChangeRecords(final Long backtrackConsumedChangeRecords) {
    this.backtrackConsumedChangeRecords = backtrackConsumedChangeRecords;
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

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(final Integer capacity) {
    this.capacity = capacity;
  }

  public String getCharacterSetName() {
    return characterSetName;
  }

  public void setCharacterSetName(final String characterSetName) {
    this.characterSetName = characterSetName;
  }

  public String getCloneGroupId() {
    return cloneGroupId;
  }

  public void setCloneGroupId(final String cloneGroupId) {
    this.cloneGroupId = cloneGroupId;
  }

  public java.util.Date getClusterCreateTime() {
    return clusterCreateTime;
  }

  public void setClusterCreateTime(final java.util.Date clusterCreateTime) {
    this.clusterCreateTime = clusterCreateTime;
  }

  public Boolean getCopyTagsToSnapshot() {
    return copyTagsToSnapshot;
  }

  public void setCopyTagsToSnapshot(final Boolean copyTagsToSnapshot) {
    this.copyTagsToSnapshot = copyTagsToSnapshot;
  }

  public Boolean getCrossAccountClone() {
    return crossAccountClone;
  }

  public void setCrossAccountClone(final Boolean crossAccountClone) {
    this.crossAccountClone = crossAccountClone;
  }

  public StringList getCustomEndpoints() {
    return customEndpoints;
  }

  public void setCustomEndpoints(final StringList customEndpoints) {
    this.customEndpoints = customEndpoints;
  }

  public String getDBClusterArn() {
    return dBClusterArn;
  }

  public void setDBClusterArn(final String dBClusterArn) {
    this.dBClusterArn = dBClusterArn;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public DBClusterMemberList getDBClusterMembers() {
    return dBClusterMembers;
  }

  public void setDBClusterMembers(final DBClusterMemberList dBClusterMembers) {
    this.dBClusterMembers = dBClusterMembers;
  }

  public DBClusterOptionGroupMemberships getDBClusterOptionGroupMemberships() {
    return dBClusterOptionGroupMemberships;
  }

  public void setDBClusterOptionGroupMemberships(final DBClusterOptionGroupMemberships dBClusterOptionGroupMemberships) {
    this.dBClusterOptionGroupMemberships = dBClusterOptionGroupMemberships;
  }

  public String getDBClusterParameterGroup() {
    return dBClusterParameterGroup;
  }

  public void setDBClusterParameterGroup(final String dBClusterParameterGroup) {
    this.dBClusterParameterGroup = dBClusterParameterGroup;
  }

  public String getDBSubnetGroup() {
    return dBSubnetGroup;
  }

  public void setDBSubnetGroup(final String dBSubnetGroup) {
    this.dBSubnetGroup = dBSubnetGroup;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(final String databaseName) {
    this.databaseName = databaseName;
  }

  public String getDbClusterResourceId() {
    return dbClusterResourceId;
  }

  public void setDbClusterResourceId(final String dbClusterResourceId) {
    this.dbClusterResourceId = dbClusterResourceId;
  }

  public Boolean getDeletionProtection() {
    return deletionProtection;
  }

  public void setDeletionProtection(final Boolean deletionProtection) {
    this.deletionProtection = deletionProtection;
  }

  public java.util.Date getEarliestBacktrackTime() {
    return earliestBacktrackTime;
  }

  public void setEarliestBacktrackTime(final java.util.Date earliestBacktrackTime) {
    this.earliestBacktrackTime = earliestBacktrackTime;
  }

  public java.util.Date getEarliestRestorableTime() {
    return earliestRestorableTime;
  }

  public void setEarliestRestorableTime(final java.util.Date earliestRestorableTime) {
    this.earliestRestorableTime = earliestRestorableTime;
  }

  public LogTypeList getEnabledCloudwatchLogsExports() {
    return enabledCloudwatchLogsExports;
  }

  public void setEnabledCloudwatchLogsExports(final LogTypeList enabledCloudwatchLogsExports) {
    this.enabledCloudwatchLogsExports = enabledCloudwatchLogsExports;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
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

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public Boolean getHttpEndpointEnabled() {
    return httpEndpointEnabled;
  }

  public void setHttpEndpointEnabled(final Boolean httpEndpointEnabled) {
    this.httpEndpointEnabled = httpEndpointEnabled;
  }

  public Boolean getIAMDatabaseAuthenticationEnabled() {
    return iAMDatabaseAuthenticationEnabled;
  }

  public void setIAMDatabaseAuthenticationEnabled(final Boolean iAMDatabaseAuthenticationEnabled) {
    this.iAMDatabaseAuthenticationEnabled = iAMDatabaseAuthenticationEnabled;
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

  public String getMasterUsername() {
    return masterUsername;
  }

  public void setMasterUsername(final String masterUsername) {
    this.masterUsername = masterUsername;
  }

  public Boolean getMultiAZ() {
    return multiAZ;
  }

  public void setMultiAZ(final Boolean multiAZ) {
    this.multiAZ = multiAZ;
  }

  public String getPercentProgress() {
    return percentProgress;
  }

  public void setPercentProgress(final String percentProgress) {
    this.percentProgress = percentProgress;
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

  public ReadReplicaIdentifierList getReadReplicaIdentifiers() {
    return readReplicaIdentifiers;
  }

  public void setReadReplicaIdentifiers(final ReadReplicaIdentifierList readReplicaIdentifiers) {
    this.readReplicaIdentifiers = readReplicaIdentifiers;
  }

  public String getReaderEndpoint() {
    return readerEndpoint;
  }

  public void setReaderEndpoint(final String readerEndpoint) {
    this.readerEndpoint = readerEndpoint;
  }

  public String getReplicationSourceIdentifier() {
    return replicationSourceIdentifier;
  }

  public void setReplicationSourceIdentifier(final String replicationSourceIdentifier) {
    this.replicationSourceIdentifier = replicationSourceIdentifier;
  }

  public ScalingConfigurationInfo getScalingConfigurationInfo() {
    return scalingConfigurationInfo;
  }

  public void setScalingConfigurationInfo(final ScalingConfigurationInfo scalingConfigurationInfo) {
    this.scalingConfigurationInfo = scalingConfigurationInfo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public Boolean getStorageEncrypted() {
    return storageEncrypted;
  }

  public void setStorageEncrypted(final Boolean storageEncrypted) {
    this.storageEncrypted = storageEncrypted;
  }

  public VpcSecurityGroupMembershipList getVpcSecurityGroups() {
    return vpcSecurityGroups;
  }

  public void setVpcSecurityGroups(final VpcSecurityGroupMembershipList vpcSecurityGroups) {
    this.vpcSecurityGroups = vpcSecurityGroups;
  }

}
