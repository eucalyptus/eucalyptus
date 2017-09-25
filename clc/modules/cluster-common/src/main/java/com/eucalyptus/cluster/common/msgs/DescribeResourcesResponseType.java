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

public class DescribeResourcesResponseType extends CloudClusterMessage {

  private ArrayList<ResourceType> resources = new ArrayList<ResourceType>( );
  private ArrayList<NodeType> nodes = new ArrayList<NodeType>( );
  private ArrayList<String> serviceTags = new ArrayList<String>( );

  public String toString( ) {
    final StringBuilder out = new StringBuilder();
    resources.forEach( resource -> {
      out.append( resource.getClass( ).getSimpleName( ) );
      out.append( ": " );
      out.append( resource );
      out.append( '\n' );
    } );
    nodes.forEach( node -> {
      out.append( node.getClass( ).getSimpleName( ) );
      out.append( ": " );
      out.append( node );
      out.append( '\n' );
    } );
    return out.toString( );
  }

  public ArrayList<ResourceType> getResources( ) {
    return resources;
  }

  public void setResources( ArrayList<ResourceType> resources ) {
    this.resources = resources;
  }

  public ArrayList<NodeType> getNodes( ) {
    return nodes;
  }

  public void setNodes( ArrayList<NodeType> nodes ) {
    this.nodes = nodes;
  }

  public ArrayList<String> getServiceTags( ) {
    return serviceTags;
  }

  public void setServiceTags( ArrayList<String> serviceTags ) {
    this.serviceTags = serviceTags;
  }
}
