/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateBlockDeviceMapping extends EucalyptusData {

  private String deviceName;
  private LaunchTemplateEbsBlockDevice ebs;
  private String noDevice;
  private String virtualName;

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( final String deviceName ) {
    this.deviceName = deviceName;
  }

  public LaunchTemplateEbsBlockDevice getEbs( ) {
    return ebs;
  }

  public void setEbs( final LaunchTemplateEbsBlockDevice ebs ) {
    this.ebs = ebs;
  }

  public String getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( final String noDevice ) {
    this.noDevice = noDevice;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( final String virtualName ) {
    this.virtualName = virtualName;
  }

}
