/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ScheduledInstancesBlockDeviceMapping extends EucalyptusData {

  private String deviceName;
  private ScheduledInstancesEbs ebs;
  private String noDevice;
  private String virtualName;

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public ScheduledInstancesEbs getEbs( ) {
    return ebs;
  }

  public void setEbs( ScheduledInstancesEbs ebs ) {
    this.ebs = ebs;
  }

  public String getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( String noDevice ) {
    this.noDevice = noDevice;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }
}
