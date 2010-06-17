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
package com.eucalyptus.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.NetworkAlreadyExistsException;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.util.Messaging;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ConfigurableClass( root = "vmstate", description = "Parameters controlling the lifecycle of virtual machines." )
public class SystemState {
  
  public static Logger  LOG            = Logger.getLogger( SystemState.class );
  @ConfigurableField( description = "Amount of time (in milliseconds) that a terminated VM will continue to be reported.", initial = "" + 60 * 60 * 1000 )
  public static Integer BURY_TIME      = -1;
  @ConfigurableField( description = "Amount of time (in milliseconds) before a VM which is not reported by a cluster will be marked as terminated.", initial = "" + 10 * 60 * 1000 )
  public static Integer SHUT_DOWN_TIME = -1;
  
  public enum Reason {
    NORMAL(""),
    EXPIRED( "Instance expired after not being reported for %s ms.", SystemState.SHUT_DOWN_TIME ),
    FAILED( "The instance failed to start on the NC." ),
    USER_TERMINATED( "User initiated terminate." ),
    BURIED( "Instance buried after timeout of %s ms.", SystemState.BURY_TIME ),
    APPEND("");
    private String   message;
    private Object[] args;
    
    Reason( String message, Object... args ) {
      this.message = message;
      this.args = args;
    }
    
    @Override
    public String toString( ) {
      return String.format( this.message.toString( ), this.args );
    }
    
  }
  
  public static void handle( VmDescribeResponseType request ) {
    VmInstances.flushBuried( );
    String originCluster = request.getOriginCluster( );
    for ( VmInfo runVm : request.getVms( ) ) {
      SystemState.updateVmInstance( originCluster, runVm );
    }
    final Set<String> unreportedVms = VmInstances.getInstance( ).getKeys( );
    final List<String> runningVmIds = Lists.transform( request.getVms( ), new Function<VmInfo, String>( ) {
      @Override
      public String apply( VmInfo arg0 ) {
        String vmId = arg0.getImageId( );
        unreportedVms.remove( vmId );
        return vmId;
      }
    } );
    for ( String vmId : unreportedVms ) {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( vmId );
        if ( vm.getSplitTime( ) > SHUT_DOWN_TIME ) {
          vm.setState( VmState.TERMINATED, Reason.EXPIRED );
        }
      } catch ( NoSuchElementException e ) {}
    }
  }
  
  private static void updateVmInstance( final String originCluster, final VmInfo runVm ) {
    VmState state = VmState.Mapper.get( runVm.getStateName( ) );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( runVm.getInstanceId( ) );
    } catch ( NoSuchElementException e ) {
      try {
        vm = VmInstances.getInstance( ).lookupDisabled( runVm.getInstanceId( ) );
        if ( !VmState.BURIED.equals( vm.getState( ) ) && vm.getSplitTime( ) > BURY_TIME ) {
          vm.setState( VmState.BURIED, Reason.BURIED );
        }
        return;
      } catch ( NoSuchElementException e1 ) {
        if ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) ) {
          SystemState.restoreInstance( originCluster, runVm );
        }
        return;
      }
    }
    long splitTime = vm.getSplitTime( );
    VmState oldState = vm.getState( );
    
    vm.setServiceTag( runVm.getServiceTag( ) );
    
    if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) && splitTime > SHUT_DOWN_TIME ) {
      vm.setState( VmState.TERMINATED, Reason.EXPIRED );
    } else if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) && VmState.SHUTTING_DOWN.equals( VmState.Mapper.get( runVm.getStateName( ) ) ) ) {
      vm.setState( VmState.TERMINATED, Reason.APPEND, "DONE" );
    } else {
      vm.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
      vm.setState( VmState.Mapper.get( runVm.getStateName( ) ), Reason.APPEND, "UPDATE" );
      vm.updateNetworkIndex( runVm.getNetParams( ).getNetworkIndex( ) );
      vm.setVolumes( runVm.getVolumes( ) );
      try {
        Network network = Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + runVm.getGroupNames( ).get( 0 ) );
        network.extantNetworkIndex( vm.getPlacement( ), vm.getNetworkIndex( ) );
      } catch ( Exception e ) {}
    }
  }
  
  private static void restoreInstance( final String cluster, final VmInfo runVm ) {
    try {
      String instanceId = runVm.getInstanceId( );
      String reservationId = runVm.getReservationId( );
      String ownerId = runVm.getOwnerId( );
      String placement = cluster;
      String userData = runVm.getUserData( );
      Integer launchIndex = 0;
      try {
        launchIndex = Integer.parseInt( runVm.getLaunchIndex( ) );
      } catch ( NumberFormatException e ) {}
      
      VmImageInfo imgInfo = null;
      //FIXME: really need to populate these asynchronously for multi-cluster/split component... 
      try {
        imgInfo = ( VmImageInfo ) Messaging.send( "vm://ImageResolve", runVm );
      } catch ( EucalyptusCloudException e ) {
        imgInfo = new VmImageInfo( runVm.getImageId( ), runVm.getKernelId( ), runVm.getRamdiskId( ), null, null, null, null );
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
      vm.clearPending( );
      vm.setLaunchTime( runVm.getLaunchTime( ) );
      vm.updatePublicAddress( VmInstance.DEFAULT_IP );
      vm.setKeyInfo( keyInfo );
      vm.setImageInfo( imgInfo );
      VmInstances.getInstance( ).register( vm );
    } catch ( NoSuchElementException e ) {
      ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
      new TerminateCallback( runVm.getInstanceId( ) ).dispatch( runVm.getPlacement( ) );
    }
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
