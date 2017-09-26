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
package com.eucalyptus.empyrean;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ServiceStatusDetail extends EucalyptusData {

  private String severity;
  private String uuid;
  private String message;
  private String serviceFullName;
  private String serviceName;
  private String serviceHost;
  private String stackTrace;
  private String timestamp;

  @Override
  public String toString( ) {
    return this.timestamp + " " + this.severity + " " + this.serviceFullName + " " + this.serviceName + " " + this.serviceHost + " " + this.message;
  }

  public String getSeverity( ) {
    return severity;
  }

  public void setSeverity( String severity ) {
    this.severity = severity;
  }

  public String getUuid( ) {
    return uuid;
  }

  public void setUuid( String uuid ) {
    this.uuid = uuid;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getServiceFullName( ) {
    return serviceFullName;
  }

  public void setServiceFullName( String serviceFullName ) {
    this.serviceFullName = serviceFullName;
  }

  public String getServiceName( ) {
    return serviceName;
  }

  public void setServiceName( String serviceName ) {
    this.serviceName = serviceName;
  }

  public String getServiceHost( ) {
    return serviceHost;
  }

  public void setServiceHost( String serviceHost ) {
    this.serviceHost = serviceHost;
  }

  public String getStackTrace( ) {
    return stackTrace;
  }

  public void setStackTrace( String stackTrace ) {
    this.stackTrace = stackTrace;
  }

  public String getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( String timestamp ) {
    this.timestamp = timestamp;
  }
}
