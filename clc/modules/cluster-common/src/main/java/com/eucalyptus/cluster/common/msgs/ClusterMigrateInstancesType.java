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
import java.util.List;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ClusterMigrateInstancesType extends CloudClusterMessage {

  private String sourceHost;
  private String instanceId;
  private ArrayList<String> resourceLocations = new ArrayList<String>( );
  private ArrayList<String> destinationHosts = new ArrayList<String>( );
  private Boolean allowHosts = false;

  public void addResourceLocations( List<VmTypeInfo> resources ) {
    resources.forEach( vmTypeInfo -> {
      ArrayList<VirtualBootRecord> images =
          Lists.newArrayList(vmTypeInfo.lookupKernel(), vmTypeInfo.lookupRamdisk(), vmTypeInfo.lookupRoot());
      images.forEach( image -> {
        if( image != null ) {
          this.resourceLocations.add(image.getId() + '=' + image.getResourceLocation());
        }
      } );
    } );
    resourceLocations = Lists.newArrayList( Sets.newLinkedHashSet( resourceLocations ) );
  }

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

  public ArrayList<String> getResourceLocations( ) {
    return resourceLocations;
  }

  public void setResourceLocations( ArrayList<String> resourceLocations ) {
    this.resourceLocations = resourceLocations;
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
