/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterSnapshot extends EucalyptusData {

  private Integer allocatedStorage;

  private AvailabilityZones availabilityZones;

  private java.util.Date clusterCreateTime;

  private String dBClusterIdentifier;

  private String dBClusterSnapshotArn;

  private String dBClusterSnapshotIdentifier;

  private String engine;

  private String engineVersion;

  private Boolean iAMDatabaseAuthenticationEnabled;

  private String kmsKeyId;

  private String licenseModel;

  private String masterUsername;

  private Integer percentProgress;

  private Integer port;

  private java.util.Date snapshotCreateTime;

  private String snapshotType;

  private String sourceDBClusterSnapshotArn;

  private String status;

  private Boolean storageEncrypted;

  private String vpcId;

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public AvailabilityZones getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(final AvailabilityZones availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  public java.util.Date getClusterCreateTime() {
    return clusterCreateTime;
  }

  public void setClusterCreateTime(final java.util.Date clusterCreateTime) {
    this.clusterCreateTime = clusterCreateTime;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getDBClusterSnapshotArn() {
    return dBClusterSnapshotArn;
  }

  public void setDBClusterSnapshotArn(final String dBClusterSnapshotArn) {
    this.dBClusterSnapshotArn = dBClusterSnapshotArn;
  }

  public String getDBClusterSnapshotIdentifier() {
    return dBClusterSnapshotIdentifier;
  }

  public void setDBClusterSnapshotIdentifier(final String dBClusterSnapshotIdentifier) {
    this.dBClusterSnapshotIdentifier = dBClusterSnapshotIdentifier;
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

  public Integer getPercentProgress() {
    return percentProgress;
  }

  public void setPercentProgress(final Integer percentProgress) {
    this.percentProgress = percentProgress;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public java.util.Date getSnapshotCreateTime() {
    return snapshotCreateTime;
  }

  public void setSnapshotCreateTime(final java.util.Date snapshotCreateTime) {
    this.snapshotCreateTime = snapshotCreateTime;
  }

  public String getSnapshotType() {
    return snapshotType;
  }

  public void setSnapshotType(final String snapshotType) {
    this.snapshotType = snapshotType;
  }

  public String getSourceDBClusterSnapshotArn() {
    return sourceDBClusterSnapshotArn;
  }

  public void setSourceDBClusterSnapshotArn(final String sourceDBClusterSnapshotArn) {
    this.sourceDBClusterSnapshotArn = sourceDBClusterSnapshotArn;
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

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
