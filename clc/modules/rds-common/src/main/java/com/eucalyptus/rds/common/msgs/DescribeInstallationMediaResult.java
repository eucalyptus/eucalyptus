/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeInstallationMediaResult extends EucalyptusData {

  private InstallationMediaList installationMedia;

  private String marker;

  public InstallationMediaList getInstallationMedia() {
    return installationMedia;
  }

  public void setInstallationMedia(final InstallationMediaList installationMedia) {
    this.installationMedia = installationMedia;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
