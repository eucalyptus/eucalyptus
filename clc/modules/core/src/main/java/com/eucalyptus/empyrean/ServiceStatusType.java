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

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ServiceStatusType extends EucalyptusData {

  private ServiceId serviceId;
  /**
   * one of DISABLED, PRIMORDIAL, INITIALIZED, LOADED, RUNNING, STOPPED, PAUSED
   **/
  private String localState;
  private Integer localEpoch;
  private ArrayList<String> details = new ArrayList<String>( );
  private ArrayList<ServiceStatusDetail> statusDetails = new ArrayList<ServiceStatusDetail>( );
  private ArrayList<ServiceAccount> serviceAccounts = new ArrayList<ServiceAccount>( );

  @Override
  public String toString( ) {
    return this.serviceId.getFullName( ) + " " + this.localState + " " + String.valueOf( this.localEpoch ) + " " + String.valueOf( this.statusDetails ) + " " + String.valueOf( this.serviceAccounts );
  }

  public ServiceId getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( ServiceId serviceId ) {
    this.serviceId = serviceId;
  }

  public String getLocalState( ) {
    return localState;
  }

  public void setLocalState( String localState ) {
    this.localState = localState;
  }

  public Integer getLocalEpoch( ) {
    return localEpoch;
  }

  public void setLocalEpoch( Integer localEpoch ) {
    this.localEpoch = localEpoch;
  }

  public ArrayList<String> getDetails( ) {
    return details;
  }

  public void setDetails( ArrayList<String> details ) {
    this.details = details;
  }

  public ArrayList<ServiceStatusDetail> getStatusDetails( ) {
    return statusDetails;
  }

  public void setStatusDetails( ArrayList<ServiceStatusDetail> statusDetails ) {
    this.statusDetails = statusDetails;
  }

  public ArrayList<ServiceAccount> getServiceAccounts( ) {
    return serviceAccounts;
  }

  public void setServiceAccounts( ArrayList<ServiceAccount> serviceAccounts ) {
    this.serviceAccounts = serviceAccounts;
  }
}
