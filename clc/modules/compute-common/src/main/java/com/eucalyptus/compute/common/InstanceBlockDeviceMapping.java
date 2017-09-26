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

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceBlockDeviceMapping extends EucalyptusData {

  private String deviceName;
  private EbsInstanceBlockDeviceMapping ebs;

  public InstanceBlockDeviceMapping( ) {
  }

  public InstanceBlockDeviceMapping( String deviceName ) {
    this.deviceName = deviceName;
  }

  public InstanceBlockDeviceMapping( String deviceName, String volumeId, String status, Date attachTime, Boolean deleteOnTerminate ) {
    this.deviceName = deviceName;
    this.ebs = new EbsInstanceBlockDeviceMapping( volumeId, status, attachTime, deleteOnTerminate );
  }

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public EbsInstanceBlockDeviceMapping getEbs( ) {
    return ebs;
  }

  public void setEbs( EbsInstanceBlockDeviceMapping ebs ) {
    this.ebs = ebs;
  }
}
