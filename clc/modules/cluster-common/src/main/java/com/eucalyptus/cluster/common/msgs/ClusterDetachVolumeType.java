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
package com.eucalyptus.cluster.common.msgs;

public class ClusterDetachVolumeType extends CloudClusterMessage {

  private String volumeId;
  private String instanceId;
  private String device;
  private String remoteDevice;
  private Boolean force = false;

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getDevice( ) {
    return device;
  }

  public void setDevice( String device ) {
    this.device = device;
  }

  public String getRemoteDevice( ) {
    return remoteDevice;
  }

  public void setRemoteDevice( String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }

  public Boolean getForce( ) {
    return force;
  }

  public void setForce( Boolean force ) {
    this.force = force;
  }
}
