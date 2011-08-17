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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cloud.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.VmRunType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NotEnoughResourcesAvailable;
import com.eucalyptus.cloud.util.Resource.SetReference;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.NoSuchTokenException;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.StartNetworkCallback;
import com.eucalyptus.cluster.callback.VmRunCallback;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.async.StatefulMessageSet;
import com.eucalyptus.vm.VmState;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.StartNetworkResponseType;
import edu.ucsb.eucalyptus.msgs.StartNetworkType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ClusterAllocator implements Runnable {
  private static Logger LOG = Logger.getLogger( ClusterAllocator.class );
  
  enum State {
    START, CREATE_VOLS, CREATE_IGROUPS, CREATE_NETWORK, CREATE_NETWORK_RULES, CREATE_VMS, ATTACH_VOLS, ASSIGN_ADDRESSES, FINISHED, ROLLBACK;
  }
  
  public static Boolean             SPLIT_REQUESTS = true; //TODO:GRZE:@Configurable
  private StatefulMessageSet<State> messages;
  private final Allocation          allocInfo;
  private Cluster                   cluster;
  
  public static void create( final ResourceToken t, final Allocation allocInfo ) {
    Threads.lookup( ClusterController.class, ClusterAllocator.class ).submit( new ClusterAllocator( allocInfo ) );
  }
  
  private ClusterAllocator( final Allocation allocInfo ) {
    this.allocInfo = allocInfo;
    try {
      this.cluster = Clusters.lookup( allocInfo.getPartition( ) );
      this.messages = new StatefulMessageSet<State>( this.cluster, State.values( ) );
      this.setupVolumeMessages( );
      
      for ( final NetworkGroup network : allocInfo.getNetworkGroups( ) ) {
        this.setupNetworkMessages( network );
      }
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        this.setupVmMessages( token );
      }
    } catch ( final Exception e ) {
      LOG.debug( e, e );
      this.allocInfo.abort( );
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        try {
          final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
          vm.setState( VmState.TERMINATED, Reason.FAILED, e.getMessage( ) );
          VmInstances.disable( vm );
        } catch ( final Exception e1 ) {
          LOG.debug( e1, e1 );
        }
      }
    }
  }
  
  private void setupVolumeMessages( ) throws NoSuchElementException, MetadataException, ExecutionException {
    if ( this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
      final ServiceConfiguration sc = Partitions.lookupService( Storage.class, this.cluster.getPartition( ) );
      final VirtualBootRecord root = this.allocInfo.getVmTypeInfo( ).lookupRoot( );
      if ( root.isBlockStorage( ) ) {
        for ( int i = 0; i < this.allocInfo.getAllocationTokens( ).size( ); i++ ) {
          final BlockStorageImageInfo imgInfo = ( ( BlockStorageImageInfo ) this.allocInfo.getBootSet( ).getMachine( ) );
          final int sizeGb = ( int ) Math.ceil( imgInfo.getImageSizeBytes( ) / ( 1024l * 1024l * 1024l ) );
          LOG.debug( "About to prepare root volume using bootable block storage: " + imgInfo + " and vbr: " + root );
          final Volume vol = Volumes.createStorageVolume( sc, this.allocInfo.getOwnerFullName( ), imgInfo.getSnapshotId( ), sizeGb, this.allocInfo.getRequest( ) );
          if ( imgInfo.getDeleteOnTerminate( ) ) {
            this.allocInfo.getTransientVolumes( ).add( vol );
          } else {
            this.allocInfo.getPersistentVolumes( ).add( vol );
          }
        }
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private void setupNetworkMessages( final NetworkGroup networkGroup ) {
    if ( networkGroup != null ) {
      final Request<StartNetworkType, StartNetworkResponseType> callback = AsyncRequests.newRequest( new StartNetworkCallback( networkGroup ) );
      this.messages.addRequest( State.CREATE_NETWORK, callback );
      EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, callback.getClass( ).getSimpleName( ), networkGroup.toString( ) ).debug( );
    }
  }
  
  private void setupVmMessages( final ResourceToken token ) throws Exception {
    final String networkName = NetworkGroups.networkingConfiguration( ).hasNetworking( )
      ? this.allocInfo.getPrimaryNetwork( ).getNaturalId( )
      : NetworkGroups.lookup( this.allocInfo.getOwnerFullName( ), NetworkGroups.defaultNetworkName( ) ).getNaturalId( );
    
    final Integer vlan = token.getAllocationInfo( ).getPrimaryNetwork( ).extantNetwork( ).getTag( );
    
    final SshKeyPair keyInfo = this.allocInfo.getSshKeyPair( );
    final VmTypeInfo vmInfo = this.allocInfo.getVmTypeInfo( );
    Request cb = null;
    try {
      final VirtualBootRecord root = vmInfo.lookupRoot( );
      final VmTypeInfo childVmInfo = this.makeVmTypeInfo( vmInfo, token.getLaunchIndex( ), root );
      cb = this.makeRunRequest( token, childVmInfo, networkName );
      this.messages.addRequest( State.CREATE_VMS, cb );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  private VmTypeInfo makeVmTypeInfo( final VmTypeInfo vmInfo, final int index, final VirtualBootRecord root ) {
    VmTypeInfo childVmInfo = vmInfo;
    if ( root.isBlockStorage( ) ) {
      childVmInfo = vmInfo.child( );
      final Volume vol = this.allocInfo.getPersistentVolumes( ).get( index );
      final Dispatcher sc = ServiceDispatcher.lookup( Partitions.lookupService( Storage.class, vol.getPartition( ) ) );
      for ( int i = 0; i < 60; i++ ) {
        try {
          final DescribeStorageVolumesResponseType volState = sc.send( new DescribeStorageVolumesType( Lists.newArrayList( vol.getDisplayName( ) ) ) );
          if ( "available".equals( volState.getVolumeSet( ).get( 0 ).getStatus( ) ) ) {
            break;
          } else {
            TimeUnit.SECONDS.sleep( 1 );
          }
        } catch ( final InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      for ( final String nodeTag : this.cluster.getNodeTags( ) ) {
        try {
          final AttachStorageVolumeResponseType scAttachResponse = sc.send( new AttachStorageVolumeType( this.cluster.getNode( nodeTag ).getIqn( ),
                                                                                                         vol.getDisplayName( ) ) );
          childVmInfo.lookupRoot( ).setResourceLocation( scAttachResponse.getRemoteDeviceString( ) );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }//TODO:GRZE:OMGFIXME: move this for bfe to later stage.
    return childVmInfo;
  }
  
  private Request makeRunRequest( final ResourceToken childToken, final VmTypeInfo vmInfo, final String networkName ) {
    final SshKeyPair keyPair = this.allocInfo.getSshKeyPair( );
    final VmKeyInfo vmKeyInfo = new VmKeyInfo( keyPair.getName( ), keyPair.getPublicKey( ), keyPair.getFingerPrint( ) );
    final String platform = this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ) != null
      ? this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( )
      : "linux"; // ASAP:FIXME:GRZE
    ExtantNetwork exNet;
    try {
      exNet = this.allocInfo.getPrimaryNetwork( ).extantNetwork( );
    } catch ( NotEnoughResourcesAvailable ex ) {
      Logs.extreme( ).error( ex, ex );
      exNet = ExtantNetwork.bogus( this.allocInfo.getPrimaryNetwork( ) );
    }
    final VmRunType run = VmRunType.builder( )
                                   .instanceId( childToken.getInstanceId( ) )
                                   .naturalId( childToken.getInstanceUuid( ) )
                                   .keyInfo( vmKeyInfo )
                                   .launchIndex( childToken.getLaunchIndex( ) )
                                   .networkIndex( childToken.getNetworkIndex( ).get( ).getIndex( ) )
                                   .networkNames( this.allocInfo.getNetworkGroups( ) )
                                   .platform( platform )
                                   .reservationId( childToken.getAllocationInfo( ).getReservationId( ) )
                                   .userData( this.allocInfo.getRequest( ).getUserData( ) )
                                   .vlan( exNet.getTag( ) )
                                   .vmTypeInfo( vmInfo )
                                   .owner( this.allocInfo.getOwnerFullName( ) )
                                   .create( );
    final Request<VmRunType, VmRunResponseType> req = AsyncRequests.newRequest( new VmRunCallback( run, childToken ) );
    if ( childToken.getAddress( ) != null ) {
      req.then( new Callback.Success<VmRunResponseType>( ) {
        @Override
        public void fire( final VmRunResponseType response ) {
          final Address addr = childToken.getAddress( );
          for ( final VmInfo vmInfo : response.getVms( ) ) {//TODO: this will have some funny failure characteristics
            final VmInstance vm = VmInstances.getInstance( ).lookup( vmInfo.getInstanceId( ) );
            AsyncRequests.newRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
              @Override
              public void fire( final BaseMessage response ) {
                vm.updatePublicAddress( addr.getName( ) );
              }
            } ).dispatch( addr.getPartition( ) );
          }
        }
      } );
    }
    return req;
  }
  
  @Override
  public void run( ) {
    this.messages.run( );
  }
  
}
