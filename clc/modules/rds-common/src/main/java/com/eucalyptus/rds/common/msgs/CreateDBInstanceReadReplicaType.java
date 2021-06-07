/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBInstanceReadReplicaType extends RdsMessage {

  private Boolean autoMinorVersionUpgrade;

  private String availabilityZone;

  private Boolean copyTagsToSnapshot;

  private String dBInstanceClass;

  @Nonnull
  private String dBInstanceIdentifier;

  private String dBParameterGroupName;

  private String dBSubnetGroupName;

  private Boolean deletionProtection;

  private String domain;

  private String domainIAMRoleName;

  private LogTypeList enableCloudwatchLogsExports;

  private Boolean enableIAMDatabaseAuthentication;

  private Boolean enablePerformanceInsights;

  private Integer iops;

  private String kmsKeyId;

  private Integer monitoringInterval;

  private String monitoringRoleArn;

  private Boolean multiAZ;

  private String optionGroupName;

  private String performanceInsightsKMSKeyId;

  private Integer performanceInsightsRetentionPeriod;

  private Integer port;

  private String preSignedUrl;

  private ProcessorFeatureList processorFeatures;

  private Boolean publiclyAccessible;

  @Nonnull
  private String sourceDBInstanceIdentifier;

  private String storageType;

  private TagList tags;

  private Boolean useDefaultProcessorFeatures;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

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

  public String getPreSignedUrl() {
    return preSignedUrl;
  }

  public void setPreSignedUrl(final String preSignedUrl) {
    this.preSignedUrl = preSignedUrl;
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

  public String getSourceDBInstanceIdentifier() {
    return sourceDBInstanceIdentifier;
  }

  public void setSourceDBInstanceIdentifier(final String sourceDBInstanceIdentifier) {
    this.sourceDBInstanceIdentifier = sourceDBInstanceIdentifier;
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
