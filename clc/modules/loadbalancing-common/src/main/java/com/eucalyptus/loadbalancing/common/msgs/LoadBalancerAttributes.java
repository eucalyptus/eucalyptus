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
package com.eucalyptus.loadbalancing.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LoadBalancerAttributes extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private CrossZoneLoadBalancing crossZoneLoadBalancing;
  private AccessLog accessLog;
  private ConnectionDraining connectionDraining;
  private ConnectionSettings connectionSettings;

  public CrossZoneLoadBalancing getCrossZoneLoadBalancing( ) {
    return crossZoneLoadBalancing;
  }

  public void setCrossZoneLoadBalancing( CrossZoneLoadBalancing crossZoneLoadBalancing ) {
    this.crossZoneLoadBalancing = crossZoneLoadBalancing;
  }

  public AccessLog getAccessLog( ) {
    return accessLog;
  }

  public void setAccessLog( AccessLog accessLog ) {
    this.accessLog = accessLog;
  }

  public ConnectionDraining getConnectionDraining( ) {
    return connectionDraining;
  }

  public void setConnectionDraining( ConnectionDraining connectionDraining ) {
    this.connectionDraining = connectionDraining;
  }

  public ConnectionSettings getConnectionSettings( ) {
    return connectionSettings;
  }

  public void setConnectionSettings( ConnectionSettings connectionSettings ) {
    this.connectionSettings = connectionSettings;
  }
}
