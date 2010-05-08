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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.AddressCategory;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.UnconditionalCallback;
import com.eucalyptus.cluster.callback.ConsoleOutputCallback;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.eucalyptus.cluster.callback.RebootCallback;
import com.eucalyptus.cluster.callback.StopNetworkCallback;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.ws.util.Messaging;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.cluster.NetworkAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import com.eucalyptus.records.EventRecord;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ConfigurableClass( alias = "vmstate", description = "Parameters controlling the lifecycle of virtual machines." )
public class SystemState {
  
  private static Logger      LOG                 = Logger.getLogger( SystemState.class );
  @ConfigurableField( description = "Amount of time (in milliseconds) that a terminated VM will continue to be reported.", initial = "" + 60 * 60 * 1000 )
  public static Integer      BURY_TIME           = -1;
  @ConfigurableField( description = "Amount of time (in milliseconds) before a VM which is not reported by a cluster will be marked as terminated.", initial = "" + 10 * 60 * 1000 )
  public static Integer      SHUT_DOWN_TIME      = -1;
  public static final String INSTANCE_EXPIRED    = "Instance no longer reported as existing.";
  public static final String INSTANCE_FAILED     = "Starting the instance failed.";
  public static final String INSTANCE_TERMINATED = "User requested shutdown.";
  
  public static void handle( VmDescribeResponseType request ) {
    VmInstances.flushBuried( );
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
      } catch ( NoSuchElementException e ) {}
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
      } else if ( vm.getSplitTime( ) > BURY_TIME && VmState.BURIED.equals( vm.getState( ) ) ) {
        VmInstances.getInstance( ).deregister( vm.getName( ) );
      }
    }
  }
  
  private static void cleanUp( final VmInstance vm ) {
    String networkFqName = !vm.getNetworks( ).isEmpty( ) ? vm.getOwnerId( ) + "-" + vm.getNetworkNames( ).get( 0 ) : null;
    Cluster cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
    int networkIndex = vm.getNetworkConfig( ).getNetworkIndex( );
    Address address = null;
    QueuedEventCallback cb = new TerminateCallback( vm.getInstanceId( ) );
    if ( Clusters.getInstance( ).hasNetworking( ) ) {
      try {
        address = Addresses.getInstance( ).lookup( vm.getNetworkConfig( ).getIgnoredPublicIp( ) );
      } catch ( NoSuchElementException e ) {} catch ( Throwable e1 ) {
        LOG.debug( e1, e1 );
      }
    }
    cb.then( SystemState.getCleanUpCallback( address, vm, networkIndex, networkFqName, cluster ) );
    cb.dispatch( cluster );
  }
  
  private static UnconditionalCallback getCleanUpCallback( final Address address, final VmInstance vm, final int networkIndex, final String networkFqName, final Cluster cluster ) {
    UnconditionalCallback cleanup = new UnconditionalCallback( ) {
      public void apply( ) {
        if ( address != null ) {
          try {
            if ( address.isSystemOwned( ) ) {
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "SYSTEM_ADDRESS", address.toString( ) ).debug( );
              Addresses.release( address );
            } else {
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "USER_ADDRESS", address.toString( ) ).debug( );
              AddressCategory.unassign( address ).dispatch( address.getCluster( ) );
            }
          } catch ( IllegalStateException e ) {} catch ( Throwable e ) {
            LOG.debug( e, e );
          }
        }
        vm.getNetworkConfig( ).setNetworkIndex( -1 );
        try {
          if ( networkFqName != null ) {
            Network net = Networks.getInstance( ).lookup( networkFqName );
            if ( networkIndex > 0 && vm.getNetworkNames( ).size( ) > 0 ) {
              net.returnNetworkIndex( networkIndex );
              EventRecord.caller( SystemState.class, EventType.VM_TERMINATING, "NETWORK_INDEX", networkFqName, Integer.toString( networkIndex ) ).debug( );
            }
            if ( !Networks.getInstance( ).lookup( networkFqName ).hasTokens( ) ) {
              StopNetworkCallback stopNet = new StopNetworkCallback( new NetworkToken( cluster.getName( ), net.getName( ), net.getNetworkName( ),
                                                                                       net.getVlan( ) ) );
              for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
                stopNet.newInstance( ).dispatch( cluster );
              }
            }
          }
        } catch ( NoSuchElementException e1 ) {} catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
      }
    };
    return cleanup;
  }
  
  private static void updateVmInstance( final String originCluster, final VmInfo runVm ) {
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( runVm.getInstanceId( ) );
      vm.setServiceTag( runVm.getServiceTag( ) );
      vm.setPlatform( runVm.getPlatform( ) );
      vm.setBundleTaskState( runVm.getBundleTaskStateName( ) );
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
        if ( !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) && !"".equals( runVm.getNetParams( ).getIpAddress( ) )
             && runVm.getNetParams( ).getIpAddress( ) != null ) {
          vm.getNetworkConfig( ).setIpAddress( runVm.getNetParams( ).getIpAddress( ) );
        }
        if ( VmInstance.DEFAULT_IP.equals( vm.getNetworkConfig( ).getIgnoredPublicIp( ) )
             && !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIgnoredPublicIp( ) ) && !"".equals( runVm.getNetParams( ).getIgnoredPublicIp( ) )
             && runVm.getNetParams( ).getIgnoredPublicIp( ) != null ) {
          vm.getNetworkConfig( ).setIgnoredPublicIp( runVm.getNetParams( ).getIgnoredPublicIp( ) );
        }
        String dnsDomain = "dns-disabled";
        try {
          dnsDomain = edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );
        } catch ( Exception e ) {}
        vm.getNetworkConfig( ).updateDns( dnsDomain );
        VmState oldState = vm.getState( );
        vm.setState( VmState.Mapper.get( runVm.getStateName( ) ) );
        if ( VmState.PENDING.equals( oldState ) && VmState.SHUTTING_DOWN.equals( vm.getState( ) ) ) {
          SystemState.cleanUp( vm );
        } else if ( vm.getNetworkConfig( ).getNetworkIndex( ) > 0 && runVm.getNetParams( ).getNetworkIndex( ) > 0
                    && ( VmState.RUNNING.equals( vm.getState( ) ) || VmState.PENDING.equals( vm.getState( ) ) ) ) {
          try {
            vm.getNetworkConfig( ).setNetworkIndex( runVm.getNetParams( ).getNetworkIndex( ) );
            Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + runVm.getGroupNames( ).get( 0 ) ).extantNetworkIndex(
                                                                                                                              vm.getPlacement( ),
                                                                                                                              vm.getNetworkConfig( )
                                                                                                                                .getNetworkIndex( ) );
          } catch ( Exception e ) {}
        }
        
        for ( AttachedVolume vol : runVm.getVolumes( ) ) {
          vol.setInstanceId( vm.getInstanceId( ) );
          vol.setStatus( "attached" );
        }
        List<AttachedVolume> oldVolumes = vm.getVolumes( );
        vm.setVolumes( runVm.getVolumes( ) );
        for ( AttachedVolume v : oldVolumes ) {
          if ( "attaching".equals( v.getStatus( ) ) && !vm.getVolumes( ).contains( v ) ) {
            vm.getVolumes( ).add( v );
          }
        }
      }
    } catch ( NoSuchElementException e ) {
      try {
        vm = VmInstances.getInstance( ).lookupDisabled( runVm.getInstanceId( ) );
        //:: TODO: dispatch terminate instance message here? :://
        long splitTime = vm.getSplitTime( );
        if ( splitTime > BURY_TIME ) vm.setState( VmState.BURIED );
      } catch ( NoSuchElementException e1 ) {
        VmState state = VmState.Mapper.get( runVm.getStateName( ) );
        if ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) ) {
          SystemState.restoreInstance( originCluster, runVm );
        }
      }
    }
  }
  
  private static void restoreInstance( final String cluster, final VmInfo runVm ) {
    try {
      String instanceId = runVm.getInstanceId( ), reservationId = runVm.getReservationId( ), ownerId = runVm.getOwnerId( ), placement = cluster, userData = runVm
                                                                                                                                                                 .getUserData( );
      int launchIndex = 0;
      try {
        launchIndex = Integer.parseInt( runVm.getLaunchIndex( ) );
      } catch ( NumberFormatException e ) {}
      
      VmImageInfo imgInfo = null;
      //FIXME: really need to populate these asynchronously for multi-cluster/split component... 
      try {
        imgInfo = ( VmImageInfo ) Messaging.send( "vm://ImageResolve", runVm );
      } catch ( EucalyptusCloudException e ) {
        imgInfo = new VmImageInfo( runVm.getImageId( ), runVm.getKernelId( ), runVm.getRamdiskId( ), null, null, null, null, runVm.getPlatform( ) );
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
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation( runVm.getOwnerId( ), netName,
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.trace( e );
          }
          notwork.extantNetworkIndex( runVm.getPlacement( ), runVm.getNetParams( ).getNetworkIndex( ) );
        } catch ( NoSuchElementException e1 ) {
          try {
            notwork = SystemState.getUserNetwork( runVm.getOwnerId( ), netName );
            networks.add( notwork );
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation( runVm.getOwnerId( ), netName,
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
            Networks.getInstance( ).registerIfAbsent( notwork, Networks.State.ACTIVE );
          } catch ( EucalyptusCloudException e ) {
            LOG.error( e );
            ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
            new TerminateCallback( runVm.getInstanceId( ) ).dispatch( runVm.getPlacement( ) );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.trace( e );
          }
        }
      }
      VmInstance vm = new VmInstance( reservationId, launchIndex, instanceId, ownerId, placement, userData, imgInfo, keyInfo, vmType, networks,
                                      Integer.toString( runVm.getNetParams( ).getNetworkIndex( ) ) );
      vm.setLaunchTime( runVm.getLaunchTime( ) );
      vm.getNetworkConfig( ).setIgnoredPublicIp( VmInstance.DEFAULT_IP );
      String dnsDomain = "dns-disabled";
      try {
        dnsDomain = edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );
      } catch ( Exception e ) {}
      vm.getNetworkConfig( ).updateDns( dnsDomain );
      vm.setKeyInfo( keyInfo );
      vm.setImageInfo( imgInfo );
      VmInstances.getInstance( ).register( vm );
    } catch ( NoSuchElementException e ) {
      ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
      new TerminateCallback( runVm.getInstanceId( ) ).dispatch( runVm.getPlacement( ) );
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
                                        new TerminateInstancesItemType( v.getInstanceId( ), v.getState( ).getCode( ), v.getState( ).getName( ),
                                                                        VmState.SHUTTING_DOWN.getCode( ), VmState.SHUTTING_DOWN.getName( ) ) );
          if ( VmState.RUNNING.equals( v.getState( ) ) || VmState.PENDING.equals( v.getState( ) ) ) {
            v.setState( VmState.SHUTTING_DOWN );
            v.resetStopWatch( );
            try {
              SystemState.cleanUp( v );
            } catch ( Throwable t ) {
              LOG.debug( t, t );
            }
          }
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
  
  public static void handle( GetConsoleOutputType request ) throws Exception {
    try {
      Cluster cluster = null;
      VmInstance v = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
      if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
        cluster = Clusters.getInstance( ).lookup( v.getPlacement( ) );
      }
      if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " is not in a running state." );
      }
      if ( cluster != null ) {
        new ConsoleOutputCallback( request ).dispatch( cluster );
      } else {
        Messaging.dispatch( "vm://ReplyQueue", new EucalyptusErrorMessageType( RequestContext.getEventContext( ).getService( ).getComponent( ).getClass( )
                                                                               .getSimpleName( ), request, "Failed to find required vm information" ) );        
      }
      return;
    } catch ( NoSuchElementException e ) {
      Messaging.dispatch( "vm://ReplyQueue", new EucalyptusErrorMessageType( RequestContext.getEventContext( ).getService( ).getComponent( ).getClass( )
                                                                                           .getSimpleName( ), request, e.getMessage( ) ) );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }

  public static RebootInstancesResponseType handle( RebootInstancesType request ) throws Exception {
    RebootInstancesResponseType reply = ( RebootInstancesResponseType ) request.getReply( );
    reply.set_return( true );
    for ( String instanceId : request.getInstancesSet( ) ) {
      try {
        VmInstance v = VmInstances.getInstance( ).lookup( instanceId );
        if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
          new RebootCallback( v.getInstanceId( ) ).regarding( request ).dispatch( v.getPlacement( ) );
        }
      } catch ( NoSuchElementException e ) {
        throw new EucalyptusCloudException( e.getMessage( ) );
      }
    }
    return reply;
  }
  
  private static String DESCRIBE_NO_DNS = "no-dns";
  private static String ALT_PREFIX      = "i-";
  
  public static ArrayList<ReservationInfoType> handle( String userId, List<String> instancesSet, boolean isAdmin ) throws Exception {
    Map<String, ReservationInfoType> rsvMap = new HashMap<String, ReservationInfoType>( );
    boolean dns = Component.dns.isLocal( ) && !( instancesSet.remove( DESCRIBE_NO_DNS ) || instancesSet.remove( ALT_PREFIX + DESCRIBE_NO_DNS ) );
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      if ( ( !isAdmin && !userId.equals( v.getOwnerId( ) ) || ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) ) ) continue;
      if ( rsvMap.get( v.getReservationId( ) ) == null ) {
        ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwnerId( ), v.getNetworkNames( ) );
        rsvMap.put( reservation.getReservationId( ), reservation );
      }
      rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( dns ) );
    }
    if ( isAdmin ) {
      for ( VmInstance v : VmInstances.getInstance( ).listDisabledValues( ) ) {
        if ( VmState.BURIED.equals( v.getState( ) ) ) continue;
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) continue;
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwnerId( ), v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( dns ) );
      }
    }
    return new ArrayList<ReservationInfoType>( rsvMap.values( ) );
  }
  
  public static Network getUserNetwork( String userId, String networkName ) throws EucalyptusCloudException {
    try {
      return NetworkGroupUtil.getUserNetworkRulesGroup( userId, networkName ).getVmNetwork( );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to find network: " + userId + "-" + networkName );
    }
  }
  
}
