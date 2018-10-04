/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class PciId extends EucalyptusData {

  private String deviceId;
  private String subsystemId;
  private String subsystemVendorId;
  private String vendorId;

  public String getDeviceId( ) {
    return deviceId;
  }

  public void setDeviceId( final String deviceId ) {
    this.deviceId = deviceId;
  }

  public String getSubsystemId( ) {
    return subsystemId;
  }

  public void setSubsystemId( final String subsystemId ) {
    this.subsystemId = subsystemId;
  }

  public String getSubsystemVendorId( ) {
    return subsystemVendorId;
  }

  public void setSubsystemVendorId( final String subsystemVendorId ) {
    this.subsystemVendorId = subsystemVendorId;
  }

  public String getVendorId( ) {
    return vendorId;
  }

  public void setVendorId( final String vendorId ) {
    this.vendorId = vendorId;
  }

}
