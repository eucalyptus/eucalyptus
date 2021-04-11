/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ImportInstallationMediaType extends RdsMessage {

  @Nonnull
  private String customAvailabilityZoneId;

  @Nonnull
  private String engine;

  @Nonnull
  private String engineInstallationMediaPath;

  @Nonnull
  private String engineVersion;

  @Nonnull
  private String oSInstallationMediaPath;

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

  public String getOSInstallationMediaPath() {
    return oSInstallationMediaPath;
  }

  public void setOSInstallationMediaPath(final String oSInstallationMediaPath) {
    this.oSInstallationMediaPath = oSInstallationMediaPath;
  }

}
