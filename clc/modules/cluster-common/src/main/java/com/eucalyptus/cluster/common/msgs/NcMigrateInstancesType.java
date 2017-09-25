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

import java.util.ArrayList;

public class NcMigrateInstancesType extends CloudNodeMessage {

  private String action;
  private String credentials;
  private ArrayList<InstanceType> instances = new ArrayList<InstanceType>( );
  private ArrayList<String> resourceLocation = new ArrayList<String>( );

  public String getAction( ) {
    return action;
  }

  public void setAction( String action ) {
    this.action = action;
  }

  public String getCredentials( ) {
    return credentials;
  }

  public void setCredentials( String credentials ) {
    this.credentials = credentials;
  }

  public ArrayList<InstanceType> getInstances( ) {
    return instances;
  }

  public void setInstances( ArrayList<InstanceType> instances ) {
    this.instances = instances;
  }

  public ArrayList<String> getResourceLocation( ) {
    return resourceLocation;
  }

  public void setResourceLocation( ArrayList<String> resourceLocation ) {
    this.resourceLocation = resourceLocation;
  }
}
