/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class GlobalCluster extends EucalyptusData {

  private String databaseName;

  private Boolean deletionProtection;

  private String engine;

  private String engineVersion;

  private String globalClusterArn;

  private String globalClusterIdentifier;

  private GlobalClusterMemberList globalClusterMembers;

  private String globalClusterResourceId;

  private String status;

  private Boolean storageEncrypted;

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(final String databaseName) {
    this.databaseName = databaseName;
  }

  public Boolean getDeletionProtection() {
    return deletionProtection;
  }

  public void setDeletionProtection(final Boolean deletionProtection) {
    this.deletionProtection = deletionProtection;
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

  public String getGlobalClusterArn() {
    return globalClusterArn;
  }

  public void setGlobalClusterArn(final String globalClusterArn) {
    this.globalClusterArn = globalClusterArn;
  }

  public String getGlobalClusterIdentifier() {
    return globalClusterIdentifier;
  }

  public void setGlobalClusterIdentifier(final String globalClusterIdentifier) {
    this.globalClusterIdentifier = globalClusterIdentifier;
  }

  public GlobalClusterMemberList getGlobalClusterMembers() {
    return globalClusterMembers;
  }

  public void setGlobalClusterMembers(final GlobalClusterMemberList globalClusterMembers) {
    this.globalClusterMembers = globalClusterMembers;
  }

  public String getGlobalClusterResourceId() {
    return globalClusterResourceId;
  }

  public void setGlobalClusterResourceId(final String globalClusterResourceId) {
    this.globalClusterResourceId = globalClusterResourceId;
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

}
