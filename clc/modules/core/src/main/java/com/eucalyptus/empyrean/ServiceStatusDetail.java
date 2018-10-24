/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
