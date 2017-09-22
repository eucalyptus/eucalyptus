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
package com.eucalyptus.blockstorage.msgs;

public class ExportVolumeType extends StorageRequestType {

  private String volumeId;
  private String token;
  private String ip;
  private String iqn;

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getToken( ) {
    return token;
  }

  public void setToken( String token ) {
    this.token = token;
  }

  public String getIp( ) {
    return ip;
  }

  public void setIp( String ip ) {
    this.ip = ip;
  }

  public String getIqn( ) {
    return iqn;
  }

  public void setIqn( String iqn ) {
    this.iqn = iqn;
  }
}
