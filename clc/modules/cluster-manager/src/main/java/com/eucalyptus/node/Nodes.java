/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.node;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import com.eucalyptus.component.*;
import com.eucalyptus.empyrean.*;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.fsm.TransitionRecord;
import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.vm.VmInstance;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.NodeType;
import static com.google.common.collect.Iterables.toArray;

public class Nodes {
  private static Logger LOG             = Logger.getLogger( Nodes.class );
  public static Long    REFRESH_TIMEOUT = TimeUnit.MINUTES.toMillis( 10 );

  static Function<String, NodeInfo> lookupNodeInfo( final ServiceConfiguration ccConfig ) {
    return new Function<String, NodeInfo>( ) {

      @Override
      public NodeInfo apply( @Nullable String ncHostOrTag ) {
        Map<String, NodeInfo> map = Clusters.lookup( ccConfig ).getNodeHostMap( );
        if ( map.containsKey( ncHostOrTag ) ) {
          return map.get( ncHostOrTag );
        } else {
          throw new NoSuchElementException( "Failed to lookup node using "
                                            + ncHostOrTag
                                            + ".  Available nodes are: "
                                            + Joiner.on( "\n" ).join( map.keySet( ) ) );
        }
      }
    };

  }

  static Function<NodeInfo, ServiceConfiguration> transformNodeInfo( final ServiceConfiguration ccConfig ) {
    return new Function<NodeInfo, ServiceConfiguration>( ) {

      @Override
      public ServiceConfiguration apply( @Nullable NodeInfo input ) {
        NodeController compId = ComponentIds.lookup( NodeController.class );
        Component comp = Components.lookup( compId );
        try {
          return comp.lookup( input.getName( ) );
        } catch ( final NoSuchElementException ex1 ) {
          URI nodeUri = URI.create( input.getServiceTag( ) );
          final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( comp.getComponentId( ) );
          ServiceConfiguration config = builder.newInstance( ccConfig.getPartition( ),
                                                             input.getName( ),
                                                             nodeUri.getHost( ),
                                                             nodeUri.getPort( ) );
          comp.setup( config );
          return config;
        }
      }
    };
  }

  private static Function<String, ServiceConfiguration> lookupNodeServiceConfiguration( ServiceConfiguration ccConfig ) {
    return Functions.compose( transformNodeInfo( ccConfig ), lookupNodeInfo( ccConfig ) );
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
    StringBuilder nodeLog = new StringBuilder( );
    /** maybe remove unreported nodes **/
    for ( String unreportedTag : unreportedTags ) {
      NodeInfo unreportedNode = clusterNodeMap.get( unreportedTag );
      if ( unreportedNode != null && ( System.currentTimeMillis( ) - unreportedNode.getLastSeen( ).getTime( ) ) > Nodes.REFRESH_TIMEOUT ) {
        Topology.destroy( Components.lookup( NodeController.class ).lookup( unreportedNode.getName() ) );
        NodeInfo removed = clusterNodeMap.remove( unreportedTag );
        nodeLog.append( "GONE:" ).append( removed.getName() ).append( ":" ).append( removed.getLastState() ).append( " " );
      }
    }
    /** add new nodes or updated existing node infos **/
    Set<NodeInfo> nodesToUpdate = Sets.newHashSet( );
    for ( final NodeType node : nodes ) {
      try {
        String serviceTag = node.getServiceTag( );
        if ( newTags.contains( serviceTag ) ) {
          clusterNodeMap.putIfAbsent( serviceTag, new NodeInfo( ccConfig.getPartition( ), node ) );
          NodeInfo nodeInfo = clusterNodeMap.get( serviceTag );
          nodeLog.append( "NEW:" ).append( nodeInfo.getName() ).append( ":" ).append( nodeInfo.getLastState() ).append( " " );
          nodesToUpdate.add( nodeInfo );
        } else if ( stillKnownTags.contains( serviceTag ) ) {
          NodeInfo nodeInfo = clusterNodeMap.get( serviceTag );
          nodeInfo.setIqn( node.getIqn( ) );
          nodeLog.append( "OLD:" ).append( nodeInfo.getName() ).append( ":" ).append( nodeInfo.getLastState() ).append( " " );
          nodesToUpdate.add( nodeInfo );
        }
      } catch ( NoSuchElementException e ) {
        LOG.error( e );
        LOG.debug( e, e );
      }
    }
    LOG.debug( "Updated node info map: " + nodeLog.toString() );
    try {
      Nodes.updateServiceConfiguration( ccConfig, nodesToUpdate );
    } catch ( Exception e ) {
        if( !Component.State.ENABLED.apply( ccConfig ))
          LOG.debug("Error while updating nodes: " + e.getMessage(), e);
    }
    /**
     * TODO:GRZE: if not present emulate {@link ClusterController.NodeController} using
     * {@link Component#setup()} TODO:GRZE: emulate update of emulate
     * {@link ClusterController.NodeController} state
     * TODO:GRZE: {@link Component#destroy()} for the NodeControllers which are not reported by the
     * CC.
     */

  }

  public static ServiceConfiguration lookup( ServiceConfiguration ccConfig, NodeInfo nodeInfo ) throws NoSuchElementException {
    return Nodes.transformNodeInfo( ccConfig ).apply( nodeInfo );
  }

  public static ServiceConfiguration lookup( ServiceConfiguration ccConfig, String hostOrTag ) throws NoSuchElementException {
    return Nodes.lookupNodeServiceConfiguration( ccConfig ).apply( hostOrTag );
  }

  private static void updateServiceConfiguration( final ServiceConfiguration ccConfig, Set<NodeInfo> nodeInfoSet ) throws NoSuchElementException {
    Function<NodeInfo, ServiceConfiguration> setupNode = ( Function<NodeInfo, ServiceConfiguration> ) new Function<NodeInfo, ServiceConfiguration>( ) {
      @Nullable
      @Override
      public ServiceConfiguration apply( @Nullable NodeInfo input ) {
        if ( Component.State.ENABLED.apply( ccConfig ) && !ccConfig.lookupStateMachine( ).isBusy( ) ) {
          ServiceConfiguration ncConfig = Nodes.lookup( ccConfig, input.getName( ) );
          Component component = Components.lookup( NodeController.class );
          if ( !component.hasService( ncConfig ) ) {
            component.setup( ncConfig );
            try {
              Topology.disable( ncConfig );
            } catch ( Exception e ) {
              LOG.debug( e, e );
            }
          }
          return ncConfig;
        }
        return Nodes.lookup( ccConfig, input.getName( ) );//GRZE: need to return something in this case, even knowing that the state is unhappy.
      }
    };
    Predicate<NodeInfo> disableNodes = ( Predicate<NodeInfo> ) new Predicate<NodeInfo>( ) {
      @Override
      public boolean apply( @Nullable NodeInfo nodeInfo ) {
        try {
          Topology.disable( Nodes.lookup( ccConfig, nodeInfo.getName( ) ) );
        } catch ( Exception e ) {}
        return true;
      }
    };
    if ( Component.State.DISABLED.ordinal( ) >= ccConfig.lookupState( ).ordinal( ) ) {
      Iterables.filter( nodeInfoSet, disableNodes );
    }

    Function<ServiceStatusType, String> statusToName = new Function<ServiceStatusType, String>( ) {
      @Nullable
      @Override
      public String apply( @Nullable ServiceStatusType status ) {
        return status.getServiceId( ).getName( );
      }
    };

    Iterable<ServiceConfiguration> nodesConfigs = Iterables.transform( nodeInfoSet, setupNode );
    if ( !nodeInfoSet.isEmpty() ) {
      DescribeServicesResponseType reply = Nodes.send( new DescribeServicesType( ), toArray( nodesConfigs, ServiceConfiguration.class ) );
      Map<String, ServiceStatusType> statusMap = Maps.uniqueIndex( reply.getServiceStatuses( ), statusToName );
      Map<String, NodeInfo> nodeInfoMap = Maps.uniqueIndex( nodeInfoSet, new Function<NodeInfo, String>( ) {
        @Nullable
        @Override
        public String apply( @Nullable NodeInfo nodeInfo ) {
          return nodeInfo.getName( );
        }
      } );

      for ( ServiceConfiguration ncConfig : nodesConfigs ) {
        Component.State reportedState = Component.State.ENABLED;
        ServiceStatusType status = statusMap.get( ncConfig.getName( ) );
        final NodeInfo nodeInfo = nodeInfoMap.get( ncConfig.getName() );
        String lastMessage = null;
        Faults.CheckException checkException = null;
        TransitionRecord<ServiceConfiguration, Component.State, Component.Transition> tr = null;
        try {
          lastMessage = Joiner.on( "\n" ).join( status.getDetails() );
          tr = ncConfig.lookupStateMachine().getTransitionRecord();
          try {
            reportedState = Component.State.valueOf( Strings.nullToEmpty( status.getLocalState( ) ).toUpperCase( ) );
            lastMessage = Joiner.on('\n').join( lastMessage, "Found service status for " + ncConfig.getName( ) + ": " + reportedState );
          } catch ( IllegalArgumentException e ) {
            lastMessage = Joiner.on('\n').join( lastMessage, "Failed to get service status for " + ncConfig.getName( ) + "; got " + status.getLocalState( ) );
          }
          if ( ncConfig.lookupStateMachine().isBusy() ) {
          //GRZE: here we skip the state update to avoid a race in the async dispatch of transitions.  log any state mismatch.
            if ( !ncConfig.lookupState().equals( reportedState ) ) {
              lastMessage = Joiner.on('\n').join( lastMessage, "Found state mismatch for node " + ncConfig.getName() + ": reported=" + reportedState + " local=" + ncConfig.getStateMachine() );
            } else {
              lastMessage = Joiner.on('\n').join( lastMessage, "Found state for node " + ncConfig.getName() + ": reported=" + reportedState + " local=" + ncConfig.getStateMachine() );
            }
          } else {
            try {
              if ( Component.State.ENABLED.equals( reportedState ) ) {
                Topology.enable( ncConfig );
              } else if ( !Component.State.STOPPED.apply( ncConfig ) ) {
                //GRZE: Only attempt to reflect the error state when the service is /not/ in the STOPPED state
                Topology.disable( ncConfig );
                if ( Component.State.NOTREADY.equals( reportedState ) ) {
                  checkException = Faults.failure( ncConfig, Joiner.on( "," ).join( status.getDetails() ) );
                }
              } else {
                Topology.stop( ncConfig );
              }
            } catch ( Exception e ) {
              LOG.debug( e, e );
              if ( checkException != null ) {
                LOG.debug( checkException );
              }
              checkException = Faults.failure( ncConfig, e, Objects.firstNonNull( checkException, Faults.advisory( ncConfig, lastMessage ) ) );
            }
          }
        } finally {
          checkException = Objects.firstNonNull( checkException, Faults.advisory( ncConfig, lastMessage ) );
          nodeInfo.touch( reportedState, lastMessage, checkException );
          Faults.submit( ncConfig, tr, checkException );
        }
      }
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
      throw new NoSuchElementException( "Failed to look up node information for "
                                        + vm.getInstanceId( )
                                        + " with service tag "
                                        + vm.getServiceTag( ) );
    } else if ( node.getIqn( ) == null ) {
      throw new NoSuchElementException( "Error looking up iqn for node "
                                        + vm.getServiceTag( )
                                        + " ("
                                        + vm.getInstanceId( )
                                        + "): node does not have an iqn." );
    } else {
      return Lists.newArrayList( node.getIqn( ) );
    }
  }

  static <T extends BaseMessage> T send( ServiceTransitionType msg, ServiceConfiguration... configsArr ) throws RuntimeException {
    ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, configsArr[0].lookupPartition() );
    if ( Component.State.ENABLED.apply( ccConfig ) && !ccConfig.lookupStateMachine( ).isBusy( ) ) {//GRZE: ensure not to trample the CC when it isn't ENABLED
      for ( ServiceId serviceId : Iterables.transform( Arrays.asList( configsArr ),
                                                       ServiceConfigurations.ServiceConfigurationToServiceId.INSTANCE ) ) {
        msg.getServices( ).add( serviceId );
      }
      try {
        return AsyncRequests.sendSync( ccConfig, msg );//GRZE: this call site is synchronous wrt other CC-bound requests.
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    } else {
      throw Exceptions.noSuchElement( "Failed to find cluster controller: " + ccConfig );
    }
  }

  static ServiceConfiguration lookupClusterController( ServiceConfiguration config ) {
    return Topology.lookup( ClusterController.class, config.lookupPartition( ) );
  }

  public static void clusterCleanup( Cluster cluster, Exception e ) {
    Threads.enqueue( NodeController.class, CleanupNodes.class, new CleanupNodes( cluster, e ) );
  }

  public static class CleanupNodes implements Callable<Collection<NodeInfo>> {
    private Cluster cluster;
    private Exception e;

    private CleanupNodes( Cluster cluster, Exception e ) {
      this.cluster = cluster;
      this.e = e;
    }

    @Override
    public Collection<NodeInfo> call() throws Exception {
      ServiceConfiguration config = cluster.getConfiguration();
      final ConcurrentNavigableMap<String,NodeInfo> nodeMap = cluster.getNodeMap();
      for ( NodeInfo nodeInfo : nodeMap.values() ) {
        try {
          ServiceConfiguration ncConfig = lookup( config, nodeInfo );
          String lastMessage = Joiner.on( '\n' ).join( nodeInfo.getLastMessage(), e.getMessage() );
          Faults.CheckException ex = Faults.failure( ncConfig, lastMessage );
          nodeInfo.touch( Component.State.NOTREADY, lastMessage, ex );
          if ( Component.State.ENABLED.apply( ncConfig ) ) {
            Topology.disable( ncConfig );
          }
        } catch ( Exception e ) {
          LOG.debug( e );
        }
      }
      return nodeMap.values();
    }
  }
}
