/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBInstanceAutomatedBackup extends EucalyptusData {

  private Integer allocatedStorage;

  private String availabilityZone;

  private String dBInstanceArn;

  private String dBInstanceIdentifier;

  private String dbiResourceId;

  private Boolean encrypted;

  private String engine;

  private String engineVersion;

  private Boolean iAMDatabaseAuthenticationEnabled;

  private java.util.Date instanceCreateTime;

  private Integer iops;

  private String kmsKeyId;

  private String licenseModel;

  private String masterUsername;

  private String optionGroupName;

  private Integer port;

  private String region;

  private RestoreWindow restoreWindow;

  private String status;

  private String storageType;

  private String tdeCredentialArn;

  private String timezone;

  private String vpcId;

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(final String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public String getDBInstanceArn() {
    return dBInstanceArn;
  }

  public void setDBInstanceArn(final String dBInstanceArn) {
    this.dBInstanceArn = dBInstanceArn;
  }

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public String getDbiResourceId() {
    return dbiResourceId;
  }

  public void setDbiResourceId(final String dbiResourceId) {
    this.dbiResourceId = dbiResourceId;
  }

  public Boolean getEncrypted() {
    return encrypted;
  }

  public void setEncrypted(final Boolean encrypted) {
    this.encrypted = encrypted;
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

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public RestoreWindow getRestoreWindow() {
    return restoreWindow;
  }

  public void setRestoreWindow(final RestoreWindow restoreWindow) {
    this.restoreWindow = restoreWindow;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
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

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
