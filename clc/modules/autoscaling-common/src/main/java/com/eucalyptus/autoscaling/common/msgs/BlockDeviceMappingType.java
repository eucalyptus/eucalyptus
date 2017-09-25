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
package com.eucalyptus.autoscaling.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class BlockDeviceMappingType extends EucalyptusData {

  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  private String virtualName;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  private String deviceName;
  private Ebs ebs;

  public BlockDeviceMappingType( ) {
  }

  public BlockDeviceMappingType( String deviceName, String virtualName, String snapshotId, Integer volumeSize ) {
    this.deviceName = deviceName;
    this.virtualName = virtualName;
    if ( snapshotId != null || volumeSize != null ) {
      Ebs ebs = new Ebs( );
      ebs.setSnapshotId( snapshotId );
      ebs.setVolumeSize( volumeSize );
      this.ebs = ebs;
    }

  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public Ebs getEbs( ) {
    return ebs;
  }

  public void setEbs( Ebs ebs ) {
    this.ebs = ebs;
  }
}
