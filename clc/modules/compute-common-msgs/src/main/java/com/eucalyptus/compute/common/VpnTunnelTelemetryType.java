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

public class VpnTunnelTelemetryType extends EucalyptusData {

  private String outsideIpAddress;
  private String status;
  private Date lastStatusChange;
  private String statusMessage;
  private Integer acceptedRouteCount;

  public String getOutsideIpAddress( ) {
    return outsideIpAddress;
  }

  public void setOutsideIpAddress( String outsideIpAddress ) {
    this.outsideIpAddress = outsideIpAddress;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getLastStatusChange( ) {
    return lastStatusChange;
  }

  public void setLastStatusChange( Date lastStatusChange ) {
    this.lastStatusChange = lastStatusChange;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public Integer getAcceptedRouteCount( ) {
    return acceptedRouteCount;
  }

  public void setAcceptedRouteCount( Integer acceptedRouteCount ) {
    this.acceptedRouteCount = acceptedRouteCount;
  }
}
