/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class RestoreDBClusterToPointInTimeType extends RdsMessage {

  private Long backtrackWindow;

  private Boolean copyTagsToSnapshot;

  @Nonnull
  private String dBClusterIdentifier;

  private String dBClusterParameterGroupName;

  private String dBSubnetGroupName;

  private Boolean deletionProtection;

  private LogTypeList enableCloudwatchLogsExports;

  private Boolean enableIAMDatabaseAuthentication;

  private String kmsKeyId;

  private String optionGroupName;

  private Integer port;

  private java.util.Date restoreToTime;

  private String restoreType;

  @Nonnull
  private String sourceDBClusterIdentifier;

  private TagList tags;

  private Boolean useLatestRestorableTime;

  private VpcSecurityGroupIdList vpcSecurityGroupIds;

  public Long getBacktrackWindow() {
    return backtrackWindow;
  }

  public void setBacktrackWindow(final Long backtrackWindow) {
    this.backtrackWindow = backtrackWindow;
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

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
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

  public java.util.Date getRestoreToTime() {
    return restoreToTime;
  }

  public void setRestoreToTime(final java.util.Date restoreToTime) {
    this.restoreToTime = restoreToTime;
  }

  public String getRestoreType() {
    return restoreType;
  }

  public void setRestoreType(final String restoreType) {
    this.restoreType = restoreType;
  }

  public String getSourceDBClusterIdentifier() {
    return sourceDBClusterIdentifier;
  }

  public void setSourceDBClusterIdentifier(final String sourceDBClusterIdentifier) {
    this.sourceDBClusterIdentifier = sourceDBClusterIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public Boolean getUseLatestRestorableTime() {
    return useLatestRestorableTime;
  }

  public void setUseLatestRestorableTime(final Boolean useLatestRestorableTime) {
    this.useLatestRestorableTime = useLatestRestorableTime;
  }

  public VpcSecurityGroupIdList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final VpcSecurityGroupIdList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

}
