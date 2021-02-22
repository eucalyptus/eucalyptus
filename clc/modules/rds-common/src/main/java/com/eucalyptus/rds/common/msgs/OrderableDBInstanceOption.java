/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class OrderableDBInstanceOption extends EucalyptusData {

  private AvailabilityZoneList availabilityZones;

  private AvailableProcessorFeatureList availableProcessorFeatures;

  private String dBInstanceClass;

  private String engine;

  private String engineVersion;

  private String licenseModel;

  private Integer maxIopsPerDbInstance;

  private Double maxIopsPerGib;

  private Integer maxStorageSize;

  private Integer minIopsPerDbInstance;

  private Double minIopsPerGib;

  private Integer minStorageSize;

  private Boolean multiAZCapable;

  private Boolean readReplicaCapable;

  private String storageType;

  private EngineModeList supportedEngineModes;

  private Boolean supportsEnhancedMonitoring;

  private Boolean supportsIAMDatabaseAuthentication;

  private Boolean supportsIops;

  private Boolean supportsKerberosAuthentication;

  private Boolean supportsPerformanceInsights;

  private Boolean supportsStorageAutoscaling;

  private Boolean supportsStorageEncryption;

  private Boolean vpc;

  public AvailabilityZoneList getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(final AvailabilityZoneList availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  public AvailableProcessorFeatureList getAvailableProcessorFeatures() {
    return availableProcessorFeatures;
  }

  public void setAvailableProcessorFeatures(final AvailableProcessorFeatureList availableProcessorFeatures) {
    this.availableProcessorFeatures = availableProcessorFeatures;
  }

  public String getDBInstanceClass() {
    return dBInstanceClass;
  }

  public void setDBInstanceClass(final String dBInstanceClass) {
    this.dBInstanceClass = dBInstanceClass;
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

  public String getLicenseModel() {
    return licenseModel;
  }

  public void setLicenseModel(final String licenseModel) {
    this.licenseModel = licenseModel;
  }

  public Integer getMaxIopsPerDbInstance() {
    return maxIopsPerDbInstance;
  }

  public void setMaxIopsPerDbInstance(final Integer maxIopsPerDbInstance) {
    this.maxIopsPerDbInstance = maxIopsPerDbInstance;
  }

  public Double getMaxIopsPerGib() {
    return maxIopsPerGib;
  }

  public void setMaxIopsPerGib(final Double maxIopsPerGib) {
    this.maxIopsPerGib = maxIopsPerGib;
  }

  public Integer getMaxStorageSize() {
    return maxStorageSize;
  }

  public void setMaxStorageSize(final Integer maxStorageSize) {
    this.maxStorageSize = maxStorageSize;
  }

  public Integer getMinIopsPerDbInstance() {
    return minIopsPerDbInstance;
  }

  public void setMinIopsPerDbInstance(final Integer minIopsPerDbInstance) {
    this.minIopsPerDbInstance = minIopsPerDbInstance;
  }

  public Double getMinIopsPerGib() {
    return minIopsPerGib;
  }

  public void setMinIopsPerGib(final Double minIopsPerGib) {
    this.minIopsPerGib = minIopsPerGib;
  }

  public Integer getMinStorageSize() {
    return minStorageSize;
  }

  public void setMinStorageSize(final Integer minStorageSize) {
    this.minStorageSize = minStorageSize;
  }

  public Boolean getMultiAZCapable() {
    return multiAZCapable;
  }

  public void setMultiAZCapable(final Boolean multiAZCapable) {
    this.multiAZCapable = multiAZCapable;
  }

  public Boolean getReadReplicaCapable() {
    return readReplicaCapable;
  }

  public void setReadReplicaCapable(final Boolean readReplicaCapable) {
    this.readReplicaCapable = readReplicaCapable;
  }

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(final String storageType) {
    this.storageType = storageType;
  }

  public EngineModeList getSupportedEngineModes() {
    return supportedEngineModes;
  }

  public void setSupportedEngineModes(final EngineModeList supportedEngineModes) {
    this.supportedEngineModes = supportedEngineModes;
  }

  public Boolean getSupportsEnhancedMonitoring() {
    return supportsEnhancedMonitoring;
  }

  public void setSupportsEnhancedMonitoring(final Boolean supportsEnhancedMonitoring) {
    this.supportsEnhancedMonitoring = supportsEnhancedMonitoring;
  }

  public Boolean getSupportsIAMDatabaseAuthentication() {
    return supportsIAMDatabaseAuthentication;
  }

  public void setSupportsIAMDatabaseAuthentication(final Boolean supportsIAMDatabaseAuthentication) {
    this.supportsIAMDatabaseAuthentication = supportsIAMDatabaseAuthentication;
  }

  public Boolean getSupportsIops() {
    return supportsIops;
  }

  public void setSupportsIops(final Boolean supportsIops) {
    this.supportsIops = supportsIops;
  }

  public Boolean getSupportsKerberosAuthentication() {
    return supportsKerberosAuthentication;
  }

  public void setSupportsKerberosAuthentication(final Boolean supportsKerberosAuthentication) {
    this.supportsKerberosAuthentication = supportsKerberosAuthentication;
  }

  public Boolean getSupportsPerformanceInsights() {
    return supportsPerformanceInsights;
  }

  public void setSupportsPerformanceInsights(final Boolean supportsPerformanceInsights) {
    this.supportsPerformanceInsights = supportsPerformanceInsights;
  }

  public Boolean getSupportsStorageAutoscaling() {
    return supportsStorageAutoscaling;
  }

  public void setSupportsStorageAutoscaling(final Boolean supportsStorageAutoscaling) {
    this.supportsStorageAutoscaling = supportsStorageAutoscaling;
  }

  public Boolean getSupportsStorageEncryption() {
    return supportsStorageEncryption;
  }

  public void setSupportsStorageEncryption(final Boolean supportsStorageEncryption) {
    this.supportsStorageEncryption = supportsStorageEncryption;
  }

  public Boolean getVpc() {
    return vpc;
  }

  public void setVpc(final Boolean vpc) {
    this.vpc = vpc;
  }

}
