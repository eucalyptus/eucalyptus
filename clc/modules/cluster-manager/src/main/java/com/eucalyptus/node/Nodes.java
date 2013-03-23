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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.NodeType;

public class Nodes {
  private static Logger LOG             = Logger.getLogger( Nodes.class );
  public static Long    REFRESH_TIMEOUT = TimeUnit.MINUTES.toMillis( 10 );
  
  private static Function<NodeInfo, ServiceConfiguration> transformNodeInfo( final ServiceConfiguration ccConfig ) {
    return new Function<NodeInfo, ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration apply( @Nullable NodeInfo input ) {
        NodeController compId = ComponentIds.lookup( NodeController.class );
        URI nodeUri = URI.create( input.getServiceTag( ) );
        Component comp = Components.lookup( compId );
        try {
          return comp.lookup( input.getName( ) );
        } catch ( final NoSuchElementException ex1 ) {
          final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( comp.getComponentId( ) );
          ServiceConfiguration config = builder.newInstance( ccConfig.getPartition( ), input.getName( ), nodeUri.getHost( ), nodeUri.getPort( ) );
          comp.setup( config );
          return config;
        }
      }
    };
    
  }
  
  public static void updateNodeInfo( ServiceConfiguration ccConfig, List<NodeType> nodes ) {
    ConcurrentNavigableMap<String, NodeInfo> clusterNodeMap = Clusters.lookup( ccConfig ).getNodeMap( );
    /** prepare key sets for comparison **/
    Set<String> knownTags = Sets.newHashSet( clusterNodeMap.keySet( ) );
    Set<String> reportedTags = Sets.newHashSet( );
    for ( final NodeType node : nodes ) {
      reportedTags.add( node.getServiceTag( ) );
    }
    /** compute intersections and differences **/
    Set<String> unreportedTags = Sets.difference( knownTags, reportedTags );
    Set<String> newTags = Sets.difference( reportedTags, knownTags );
    Set<String> stillKnownTags = Sets.intersection( knownTags, reportedTags );
    /** maybe remove unreported nodes **/
    for ( String unreportedTag : unreportedTags ) {
      NodeInfo unreportedNode = clusterNodeMap.get( unreportedTag );
      if ( unreportedNode != null && ( System.currentTimeMillis( ) - unreportedNode.getLastSeen( ).getTime( ) ) > Nodes.REFRESH_TIMEOUT ) {
        clusterNodeMap.remove( unreportedTag );
      }
    }
    /** add new nodes or updated existing node infos **/
    for ( final NodeType node : nodes ) {
      String serviceTag = node.getServiceTag( );
      if ( newTags.contains( serviceTag ) ) {
        clusterNodeMap.putIfAbsent( serviceTag, new NodeInfo( ccConfig.getPartition( ), node ) );
        NodeInfo nodeInfo = clusterNodeMap.get( serviceTag );
        Nodes.updateServiceConfiguration( ccConfig, nodeInfo );
        
      } else if ( stillKnownTags.contains( serviceTag ) ) {
        NodeInfo nodeInfo = clusterNodeMap.get( serviceTag );
        nodeInfo.touch( );
        nodeInfo.setIqn( serviceTag );
      }
    }
    /**
     * TODO:GRZE: if not present emulate {@link ClusterController.NodeController} using
     * {@link Component#setup()} TODO:GRZE: emulate update of emulate
     * {@link ClusterController.NodeController} state
     * TODO:GRZE: {@link Component#destroy()} for the NodeControllers which are not reported by the
     * CC.
     */
    
  }

  private static void updateServiceConfiguration( ServiceConfiguration ccConfig, NodeInfo nodeInfo ) throws NoSuchElementException {
    //GRZE:TODO:MAINTMODE: finish up here
    ServiceConfiguration ncConfig = Nodes.transformNodeInfo( ccConfig ).apply( nodeInfo );
    
    Component component = Components.lookup( NodeController.class );
    if ( !component.hasService( ncConfig ) ) {
      component.setup( ncConfig );
    } else {
    }
  }
  
  /**
   * GRZE:TODO: should return the node's service configuration
   * 
   * @param ccConfig
   * @param ncHostOrTag
   * @return
   */
  public static NodeInfo lookupNodeInfo( ServiceConfiguration ccConfig, String ncHostOrTag ) {
    Map<String, NodeInfo> map = Clusters.lookup( ccConfig ).getNodeHostMap( );
    if ( map.containsKey( ncHostOrTag ) ) {
      return map.get( ncHostOrTag );
    } else {
      throw new NoSuchElementException( "Failed to lookup node using " + ncHostOrTag + ".  Available nodes are: " + Joiner.on( "\n" ).join( map.keySet( ) ) );
    }
  }
  
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
