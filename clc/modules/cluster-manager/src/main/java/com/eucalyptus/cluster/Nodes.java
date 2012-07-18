/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cluster;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.vm.VmInstance;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.NodeInfo;

public class Nodes {
  public static List<String> lookupIqns( ServiceConfiguration ccConfig ) {
    Cluster cluster = Clusters.lookup( ccConfig );
    Set<String> ret = Sets.newHashSet( );
    for ( NodeInfo node : cluster.getNodeMap( ).values( ) ) {
      if ( node.getIqn( ) != null ) {
        ret.add( node.getIqn( ) );
      }
    }
    return Lists.newArrayList( ret );
  }
  
  public static List<String> lookupIqn( VmInstance vm ) {
    ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
    Cluster cluster = Clusters.lookup( ccConfig );
    NodeInfo node = cluster.getNode( vm.getServiceTag( ) );
    if ( node == null ) {
      throw new NoSuchElementException( "Failed to look up node information for " + vm.getInstanceId( ) + " with service tag " + vm.getServiceTag( ) );
    } else if ( node.getIqn( ) == null ) {
      throw new NoSuchElementException( "Error looking up iqn for node " + vm.getServiceTag( ) + " (" + vm.getInstanceId( ) + "): node does not have an iqn." );
    } else {
      return Lists.newArrayList( node.getIqn( ) );
    }
  }
}
