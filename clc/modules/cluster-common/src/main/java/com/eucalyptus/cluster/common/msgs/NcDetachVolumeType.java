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

public class NcDetachVolumeType extends CloudNodeMessage {

  private String instanceId;
  private String volumeId;
  private String remoteDev;
  private String localDev;
  private Boolean force;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getRemoteDev( ) {
    return remoteDev;
  }

  public void setRemoteDev( String remoteDev ) {
    this.remoteDev = remoteDev;
  }

  public String getLocalDev( ) {
    return localDev;
  }

  public void setLocalDev( String localDev ) {
    this.localDev = localDev;
  }

  public Boolean getForce( ) {
    return force;
  }

  public void setForce( Boolean force ) {
    this.force = force;
  }
}
