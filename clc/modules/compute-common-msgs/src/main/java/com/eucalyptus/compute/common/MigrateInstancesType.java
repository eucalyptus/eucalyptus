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

import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;


public class MigrateInstancesType extends CloudTopologyMessage {

  private String sourceHost;
  private String instanceId;
  @HttpParameterMapping( parameter = "DestinationHost" )
  private ArrayList<String> destinationHosts = new ArrayList<String>( );
  private Boolean allowHosts = false;

  public String getSourceHost( ) {
    return sourceHost;
  }

  public void setSourceHost( String sourceHost ) {
    this.sourceHost = sourceHost;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public ArrayList<String> getDestinationHosts( ) {
    return destinationHosts;
  }

  public void setDestinationHosts( ArrayList<String> destinationHosts ) {
    this.destinationHosts = destinationHosts;
  }

  public Boolean getAllowHosts( ) {
    return allowHosts;
  }

  public void setAllowHosts( Boolean allowHosts ) {
    this.allowHosts = allowHosts;
  }
}
