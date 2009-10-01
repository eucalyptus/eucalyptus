/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.ws;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterMessageQueue;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.net.util.AddressUtil;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.*;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.constants.VmState;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.drools.*;
import org.mule.RequestContext;

import com.eucalyptus.ws.util.Messaging;

import java.util.*;

public class SystemState {
  
  private static Logger       LOG                 = Logger.getLogger( SystemState.class );
  public static final int     BURY_TIME           = 60 * 60 * 1000;
  private static final int    SHUT_DOWN_TIME      = 2 * 60 * 1000;
  private static final String RULE_FILE           = "/rules/describe/instances.drl";
  private static final String INSTANCE_EXPIRED    = "Instance no longer reported as existing.";
  private static final String INSTANCE_TERMINATED = "User requested shutdown.";
  
  public static void handle( VmDescribeResponseType request ) {
    String originCluster = request.getOriginCluster( );
    
    for ( VmInfo runVm : request.getVms( ) )
      SystemState.updateVmInstance( originCluster, runVm );
    
    List<String> runningVmIds = new ArrayList<String>( );
    for ( VmInfo runVm : request.getVms( ) ) {
      runningVmIds.add( runVm.getInstanceId( ) );
    }
    for ( String vmId : VmInstances.getInstance( ).getKeys( ) ) {
      if ( runningVmIds.contains( vmId ) ) {
        continue;
      }
      VmInstance vm = null;
      try {
        vm = VmInstances.getInstance( ).lookup( vmId );
        long splitTime = vm.getSplitTime( );
        if ( splitTime > SHUT_DOWN_TIME ) {
          VmInstances.getInstance( ).disable( vm.getName( ) );
          vm.resetStopWatch( );
          vm.setState( VmState.TERMINATED );
          vm.setReason( INSTANCE_EXPIRED );
          SystemState.cleanUp( vm );
        }
      } catch ( NoSuchElementException e ) {
        /* should never happen, just pulled the key set, if it does ignore it */
      }
    }
    
    List<String> knownVmIds = new ArrayList<String>( );
    knownVmIds.addAll( VmInstances.getInstance( ).getKeys( ) );
    if ( knownVmIds.removeAll( runningVmIds ) ) {//<-- active registered vms not reported in describe
      for ( String vmId : knownVmIds ) {
        VmInstance vm = null;
        try {
          vm = VmInstances.getInstance( ).lookup( vmId );
          long splitTime = vm.getSplitTime( );
          if ( splitTime > SHUT_DOWN_TIME ) {
            VmInstances.getInstance( ).disable( vm.getName( ) );
            vm.resetStopWatch( );
            vm.setState( VmState.TERMINATED );
            vm.setReason( INSTANCE_EXPIRED );
            SystemState.cleanUp( vm );
          }
        } catch ( NoSuchElementException e ) {
          /* should never happen, just pulled the key set, if it does ignore it */
        }
      }
    }
    
    for ( VmInstance vm : VmInstances.getInstance( ).getDisabledEntries( ) ) {
      if ( vm.getSplitTime( ) > SHUT_DOWN_TIME && !VmState.BURIED.equals( vm.getState( ) ) ) {
        vm.setState( VmState.BURIED );
        SystemState.cleanUp( vm );
      }
    }
  }
  
  private static void cleanUp( final VmInstance vm ) {
    try {
      Clusters.dispatchClusterEvent( vm.getPlacement( ), new TerminateCallback( ),
                                     Admin.makeMsg( TerminateInstancesType.class, vm.getInstanceId( ) ) );
      try {} catch ( Exception e ) {}
    } catch ( Exception e ) {
      LOG.debug( e );
    }
  }
  
  private static void returnPublicAddress( final VmInstance vm ) {
    try {
      LOG.debug( EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, vm.getInstanceId( ) ) );
      Address address = Addresses.getInstance( ).lookup( vm.getNetworkConfig( ).getIgnoredPublicIp( ) );
      if(vm.getNetworkConfig( ).getIpAddress( ).equals( address.getInstanceAddress( ) ) ) {
        if ( address.isSystemAllocated( ) ) {
          LOG.debug( EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "SYSTEM_ADDRESS", address.toString( ) ) );
          AddressUtil.releaseAddress( address );
        } else {
          try {
            if ( address.isAssigned( ) ) {
              LOG.debug( EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "USER_ADDRESS", address.toString( ) ) );
              AddressUtil.unassignAddressFromVm( address, vm );
            }
          } catch ( Throwable e ) {
            LOG.debug( e, e );
          }
        }
        
      }
    } catch ( NoSuchElementException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  private static void returnNetworkIndex( final VmInstance vm ) {
    try {
      String networkFqName = vm.getOwnerId( ) + "-" + vm.getNetworkNames( ).get( 0 );
      LOG.debug( EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "NETWORK_INDEX", networkFqName, Integer.toString( vm.getNetworkIndex( ) ) ) );
      Networks.getInstance( ).lookup( networkFqName ).returnNetworkIndex( vm.getNetworkIndex( ) );
    } catch ( NoSuchElementException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  private static void updateVmInstance( final String originCluster, final VmInfo runVm ) {
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( runVm.getInstanceId( ) );
      if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) ) {
        long splitTime = vm.getSplitTime( );
        if ( splitTime > SHUT_DOWN_TIME ) {
          VmInstances.getInstance( ).disable( vm.getName( ) );
          vm.setState( VmState.TERMINATED );
          vm.resetStopWatch( );
          vm.setReason( INSTANCE_EXPIRED );
          SystemState.cleanUp( vm );
        } else if ( VmState.SHUTTING_DOWN.equals( VmState.Mapper.get( runVm.getStateName( ) ) ) ) {
          VmInstances.getInstance( ).disable( vm.getName( ) );
          vm.setState( VmState.TERMINATED );
          vm.resetStopWatch( );
          vm.setReason( INSTANCE_TERMINATED );
          SystemState.cleanUp( vm );
        } else return;
      } else {
        vm.resetStopWatch( );
        if ( !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) && !"".equals( runVm.getNetParams( ).getIpAddress( ) ) && runVm.getNetParams( ).getIpAddress( ) != null ) {
          vm.getNetworkConfig( ).setIpAddress(runVm.getNetParams( ).getIpAddress( ) );
        }
        if ( VmInstance.DEFAULT_IP.equals( vm.getNetworkConfig( ).getIgnoredPublicIp( ) ) && !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIgnoredPublicIp( ) ) && !"".equals( runVm.getNetParams( ).getIgnoredPublicIp( ) ) && runVm.getNetParams( ).getIgnoredPublicIp( ) != null ) {
          vm.getNetworkConfig( ).setIgnoredPublicIp(runVm.getNetParams( ).getIgnoredPublicIp( ) );
        }
        VmState oldState = vm.getState( );
        vm.setState( VmState.Mapper.get( runVm.getStateName( ) ) );
        if( VmState.PENDING.equals( oldState ) && VmState.SHUTTING_DOWN.equals( vm.getState( ) ) ) {
          SystemState.returnNetworkIndex( vm );
          SystemState.returnPublicAddress( vm );
        }
        for ( AttachedVolume vol : runVm.getVolumes( ) ) {
          vol.setInstanceId( vm.getInstanceId( ) );
          vol.setStatus( "attached" );
        }
        vm.setVolumes( runVm.getVolumes( ) );
        //        if ( VmState.RUNNING.equals( vm.getState( ) ) || VmState.PENDING.equals( vm.getState( ) ) ) {
        //          try {
        //            Networks.getInstance( ).lookup( vm.getNetworkNames( ).get( 0 ) ).extantNetworkIndex( vm.getPlacement( ),
        //                                                                                                 vm.getNetworkIndex( ) );
        //          } catch ( Exception e ) {}
        //        } else {
        //          try {
        //            Networks.getInstance( ).lookup( vm.getNetworkNames( ).get( 0 ) ).returnNetworkIndex( vm.getNetworkIndex( ) );
        //          } catch ( Exception e ) {}
        //        }
      }
    } catch ( NoSuchElementException e ) {
      try {
        vm = VmInstances.getInstance( ).lookupDisabled( runVm.getInstanceId( ) );
        //:: TODO: dispatch terminate instance message here? :://
        long splitTime = vm.getSplitTime( );
        if ( splitTime > BURY_TIME ) vm.setState( VmState.BURIED );
      } catch ( NoSuchElementException e1 ) {
        SystemState.restoreInstance( originCluster, runVm );
      }
    }
  }
  
  private static void restoreInstance( final String cluster, final VmInfo runVm ) {
    try {
      String instanceId = runVm.getInstanceId( ), reservationId = runVm.getReservationId( ), ownerId = runVm.getOwnerId( ), placement = cluster, userData = runVm.getUserData( );
      int launchIndex = 0;
      try {
        launchIndex = Integer.parseInt( runVm.getLaunchIndex( ) );
      } catch ( NumberFormatException e ) {}
      
      //:: TODO: populate these asynchronously... :://
      VmImageInfo imgInfo = null;
      try {
        imgInfo = ( VmImageInfo ) Messaging.send( "vm://ImageResolve", runVm );
      } catch ( EucalyptusCloudException e ) {
        imgInfo = new VmImageInfo( runVm.getImageId( ), runVm.getKernelId( ), runVm.getRamdiskId( ), null, null, null,
          null );
      }
      VmKeyInfo keyInfo = null;
      try {
        keyInfo = ( VmKeyInfo ) Messaging.send( "vm://KeyPairResolve", runVm );
      } catch ( EucalyptusCloudException e ) {
        keyInfo = new VmKeyInfo( "unknown", runVm.getKeyValue( ), null );
      }
      VmTypeInfo vmType = runVm.getInstanceType( );
      List<Network> networks = new ArrayList<Network>( );
      
      for ( String netName : runVm.getGroupNames( ) ) {
        Network notwork = null;
        try {
          notwork = Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + netName );
          networks.add( notwork );
          try {
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation(
                                                                                                                          runVm.getOwnerId( ),
                                                                                                                          netName,
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.error( e );
          }
          //          notwork.extantNetworkIndex( runVm.getPlacement( ), runVm.getNetworkIndex( ) );
        } catch ( NoSuchElementException e1 ) {
          try {
            notwork = SystemState.getUserNetwork( runVm.getOwnerId( ), netName );
            networks.add( notwork );
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation(
                                                                                                                          runVm.getOwnerId( ),
                                                                                                                          netName,
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
            Networks.getInstance( ).registerIfAbsent( notwork, Networks.State.ACTIVE );
          } catch ( EucalyptusCloudException e ) {
            LOG.error( e );
            ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
            SystemState.dispatch( runVm.getPlacement( ), new TerminateCallback( ),
                                  Admin.makeMsg( TerminateInstancesType.class, runVm.getInstanceId( ) ) );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.error( e );
          }
        }
      }
      VmInstance vm = new VmInstance( reservationId, launchIndex, instanceId, ownerId, placement, userData, imgInfo,
        keyInfo, vmType, networks, Integer.toString( runVm.getNetworkIndex( ) ) );
      vm.setLaunchTime( runVm.getLaunchTime( ) );
      vm.getNetworkConfig( ).setIgnoredPublicIp( VmInstance.DEFAULT_IP );
      vm.setKeyInfo( keyInfo );
      vm.setImageInfo( imgInfo );
      VmInstances.getInstance( ).register( vm );
    } catch ( NoSuchElementException e ) {
      ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
      SystemState.dispatch( runVm.getPlacement( ), new TerminateCallback( ),
                            Admin.makeMsg( TerminateInstancesType.class, runVm.getInstanceId( ) ) );
    }
  }
  
  public static TerminateInstancesResponseType handle( TerminateInstancesType request ) throws Exception {
    TerminateInstancesResponseType reply = ( TerminateInstancesResponseType ) request.getReply( );
    reply.set_return( true );
    
    for ( String instanceId : request.getInstancesSet( ) ) {
      try {
        VmInstance v = VmInstances.getInstance( ).lookup( instanceId );
        if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
          reply.getInstancesSet( ).add(
                                        new TerminateInstancesItemType( v.getInstanceId( ), v.getState( ).getCode( ),
                                          v.getState( ).getName( ), VmState.SHUTTING_DOWN.getCode( ),
                                          VmState.SHUTTING_DOWN.getName( ) ) );
          v.setState( VmState.SHUTTING_DOWN );
          v.resetStopWatch( );
          SystemState.returnNetworkIndex( v );
          SystemState.cleanUp( v );
          SystemState.returnPublicAddress( v );
          SystemState.checkNetwork( v );
        }
      } catch ( NoSuchElementException e ) {
        try {
          VmInstance v = VmInstances.getInstance( ).lookupDisabled( instanceId );
          v.setState( VmState.BURIED );
        } catch ( NoSuchElementException e1 ) {
          //no such instance.
        }
      }
    }
    return reply;
  }
  
  private static <E extends EucalyptusMessage> void dispatch( String clusterName, QueuedEventCallback event, E msg ) {
    Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
    ClusterMessageQueue clusterMq = cluster.getMessageQueue( );
    clusterMq.enqueue( QueuedEvent.make( event, msg ) );
  }
  
  private static void checkNetwork( VmInstance vm ) {
    Network net = vm.getNetworks( ).get( 0 );
    for( Cluster cluster : Clusters.getInstance( ).listValues( ) ) {
      if( net.hasToken( cluster.getName( ) ) && net.getClusterToken( cluster.getName( ) ).getIndexes( ).isEmpty( ) ) {
        try {
          net.removeToken( cluster.getName( ) );
        } catch ( NoSuchElementException e1 ) {
          LOG.debug( e1, e1 );
        }
      }
    }
    try {
      Cluster cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
      if( !net.hasTokens( ) ) {
        Clusters.dispatchClusterEvent( cluster, new StopNetworkCallback( new NetworkToken( cluster.getName( ), vm.getOwnerId( ), net.getNetworkName( ), net.getVlan( ) ) ) );
      }
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
    }
  }
  
  private static void dispatchReboot( final String clusterName, final String instanceId, final EucalyptusMessage request ) {
    Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
    QueuedEvent<RebootInstancesType> event = QueuedEvent.make( new RebootCallback( ),
                                                               Admin.makeMsg( RebootInstancesType.class, instanceId ) );
    cluster.getMessageQueue( ).enqueue( event );
  }
  
  public static void handle( GetConsoleOutputType request ) throws Exception {
    GetConsoleOutputResponseType reply = ( GetConsoleOutputResponseType ) request.getReply( );
    reply.set_return( true );
    try {
      Cluster cluster = null;
      VmInstance v = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
      if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
        cluster = Clusters.getInstance( ).lookup( v.getPlacement( ) );
      }
      if ( !VmState.RUNNING.equals( v.getState( ) ) ) throw new NoSuchElementException(
        "Instance " + request.getInstanceId( ) + " is not in a running state." );
      QueuedEvent<GetConsoleOutputType> event = QueuedEvent.make( new ConsoleOutputCallback( ), request );
      cluster.getMessageQueue( ).enqueue( event );
      return;
    } catch ( NoSuchElementException e ) {
      Messaging.dispatch( "vm://ReplyQueue", new EucalyptusErrorMessageType(
        RequestContext.getEventContext( ).getService( ).getComponent( ).getClass( ).getSimpleName( ), request,
        e.getMessage( ) ) );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public static RebootInstancesResponseType handle( RebootInstancesType request ) throws Exception {
    RebootInstancesResponseType reply = ( RebootInstancesResponseType ) request.getReply( );
    reply.set_return( true );
    if ( request.isAdministrator( ) ) {
      for ( String instanceId : request.getInstancesSet( ) ) {
        try {
          VmInstance v = VmInstances.getInstance( ).lookup( instanceId );
          SystemState.dispatchReboot( v.getPlacement( ), v.getInstanceId( ), new INTERNAL( ) );
        } catch ( NoSuchElementException e ) {
          throw new EucalyptusCloudException( e.getMessage( ) );
        }
        return reply;
      }
    }
    
    StateSnapshot state = SystemState.getSnapshot( RULE_FILE );
    try {
      QueryResults res = state.findInstances( request.getUserId( ), request.getInstancesSet( ) );
      if ( res.size( ) == 0 ) {
        reply.set_return( false );
        return reply;
      }
      Iterator iter = res.iterator( );
      while ( iter.hasNext( ) ) {
        QueryResult result = ( QueryResult ) iter.next( );
        VmInstance v = ( VmInstance ) result.get( "vm" );
        SystemState.dispatchReboot( v.getPlacement( ), v.getInstanceId( ), new INTERNAL( ) );
      }
    } finally {
      state.destroy( );
    }
    return reply;
  }
  
  public static ArrayList<ReservationInfoType> handle( String userId, List<String> instancesSet, boolean isAdmin ) throws Exception {
    Map<String, ReservationInfoType> rsvMap = new HashMap<String, ReservationInfoType>( );
    if ( isAdmin ) {
      for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) continue;
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwnerId( ),
            v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( ) );
      }
      for ( VmInstance v : VmInstances.getInstance( ).listDisabledValues( ) ) {
        if ( VmState.BURIED.equals( v.getState( ) ) ) continue;
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) continue;
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwnerId( ),
            v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( ) );
      }
      return new ArrayList<ReservationInfoType>( rsvMap.values( ) );
    }
    
    StateSnapshot state = SystemState.getSnapshot( RULE_FILE );
    for ( VmInstance v : VmInstances.getInstance( ).getDisabledEntries( ) )
      if ( !VmState.BURIED.equals( v.getState( ) ) ) state.insert( v );
    
    try {
      QueryResults res = state.findInstances( userId, instancesSet );
      Iterator iter = res.iterator( );
      while ( iter.hasNext( ) ) {
        QueryResult result = ( QueryResult ) iter.next( );
        VmInstance v = ( VmInstance ) result.get( "vm" );
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwnerId( ),
            v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( ) );
      }
    } finally {
      state.destroy( );
    }
    return new ArrayList<ReservationInfoType>( rsvMap.values( ) );
  }
  
  public static StateSnapshot getSnapshot( String rules ) throws Exception {
    return new StateSnapshot( rules );
  }
  
  public static Network getUserNetwork( String userId, String networkName ) throws EucalyptusCloudException {
    try {
      return NetworkGroupUtil.getUserNetworkRulesGroup( userId, networkName ).getVmNetwork( );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to find network: " + userId + "-" + networkName );
    }
  }
  
}
