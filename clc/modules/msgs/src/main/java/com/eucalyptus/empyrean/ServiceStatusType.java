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
