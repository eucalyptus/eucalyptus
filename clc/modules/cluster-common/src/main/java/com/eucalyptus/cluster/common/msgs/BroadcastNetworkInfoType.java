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

public class BroadcastNetworkInfoType extends CloudClusterMessage {

  private String version;
  private String appliedVersion;
  private String networkInfo;

  public BroadcastNetworkInfoType( ) {
  }

  public BroadcastNetworkInfoType(
      final String version,
      final String appliedVersion,
      final String networkInfo
  ) {
    this.version = version;
    this.appliedVersion = appliedVersion;
    this.networkInfo = networkInfo;
  }

  public String getVersion( ) {
    return version;
  }

  public void setVersion( String version ) {
    this.version = version;
  }

  public String getAppliedVersion( ) {
    return appliedVersion;
  }

  public void setAppliedVersion( String appliedVersion ) {
    this.appliedVersion = appliedVersion;
  }

  public String getNetworkInfo( ) {
    return networkInfo;
  }

  public void setNetworkInfo( String networkInfo ) {
    this.networkInfo = networkInfo;
  }
}
