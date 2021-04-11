/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteInstallationMediaResult extends EucalyptusData {

  private String customAvailabilityZoneId;

  private String engine;

  private String engineInstallationMediaPath;

  private String engineVersion;

  private InstallationMediaFailureCause failureCause;

  private String installationMediaId;

  private String oSInstallationMediaPath;

  private String status;

  public String getCustomAvailabilityZoneId() {
    return customAvailabilityZoneId;
  }

  public void setCustomAvailabilityZoneId(final String customAvailabilityZoneId) {
    this.customAvailabilityZoneId = customAvailabilityZoneId;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(final String engine) {
    this.engine = engine;
  }

  public String getEngineInstallationMediaPath() {
    return engineInstallationMediaPath;
  }

  public void setEngineInstallationMediaPath(final String engineInstallationMediaPath) {
    this.engineInstallationMediaPath = engineInstallationMediaPath;
  }

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
  }

  public InstallationMediaFailureCause getFailureCause() {
    return failureCause;
  }

  public void setFailureCause(final InstallationMediaFailureCause failureCause) {
    this.failureCause = failureCause;
  }

  public String getInstallationMediaId() {
    return installationMediaId;
  }

  public void setInstallationMediaId(final String installationMediaId) {
    this.installationMediaId = installationMediaId;
  }

  public String getOSInstallationMediaPath() {
    return oSInstallationMediaPath;
  }

  public void setOSInstallationMediaPath(final String oSInstallationMediaPath) {
    this.oSInstallationMediaPath = oSInstallationMediaPath;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
