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
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.VmRunType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Nodes;
import com.eucalyptus.cluster.callback.StartNetworkCallback;
import com.eucalyptus.cluster.callback.VmRunCallback;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.async.StatefulMessageSet;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.Reason;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmVolumeAttachment;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.StorageVolume;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import groovyjarjarantlr.collections.List;

public class ClusterAllocator implements Runnable {
  private static final long BYTES_PER_GB = ( 1024L * 1024L * 1024L );
  private static Logger     LOG          = Logger.getLogger( ClusterAllocator.class );
  
  enum State {
    START,
    CREATE_VOLS,
    CREATE_IGROUPS,
    CREATE_NETWORK,
    CREATE_NETWORK_RULES,
    CREATE_VMS,
    ATTACH_VOLS,
    ASSIGN_ADDRESSES,
    FINISHED,
    ROLLBACK;
  }
  
  public static Boolean             SPLIT_REQUESTS = true; //TODO:GRZE:@Configurable
  private StatefulMessageSet<State> messages;
  private final Allocation          allocInfo;
  private Cluster                   cluster;
  
  enum SubmitAllocation implements Predicate<Allocation> {
    INSTANCE;
    
    @Override
    public boolean apply( final Allocation allocInfo ) {
      try {
        EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, LogUtil.dumpObject( allocInfo ) ).info( );
        final ServiceConfiguration config = Topology.lookup( ClusterController.class, allocInfo.getPartition( ) );
        final Callable<Boolean> runnable = new Callable<Boolean>( ) {
          @Override
          public Boolean call( ) {
            try {
              new ClusterAllocator( allocInfo ).run( );
            } catch ( final Exception ex ) {
              LOG.warn( "Failed to prepare allocator for: " + allocInfo.getAllocationTokens( ) );
              LOG.error( "Failed to prepare allocator for: " + allocInfo.getAllocationTokens( ), ex );
            }
            return Boolean.TRUE;
          }
        };
        Threads.enqueue( config, 32, runnable );
        return true;
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  };
  
  public static Predicate<Allocation> get( ) {
    return SubmitAllocation.INSTANCE;
  }
  
  private ClusterAllocator( final Allocation allocInfo ) {
    this.allocInfo = allocInfo;
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      this.cluster = Clusters.lookup( Topology.lookup( ClusterController.class, allocInfo.getPartition( ) ) );
      this.messages = new StatefulMessageSet<State>( this.cluster, State.values( ) );
      this.setupVolumeMessages( );
      this.setupNetworkMessages( );
      db.commit( );
    } catch ( final Exception e ) {
      db.rollback( );
      LOG.error( e );
      Logs.extreme( ).error( e, e );
      this.allocInfo.abort( );
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        try {
          final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
          if ( VmState.STOPPED.equals( vm.getLastState( ) ) ) {
            VmInstances.stopped( vm );
          } else {
            VmInstances.terminated( vm );
            VmInstances.terminated( vm );
          }
        } catch ( final Exception e1 ) {
          LOG.error( e1 );
          Logs.extreme( ).error( e1, e1 );
        }
      }
      return;
    }
    
    try {
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        this.setupVmMessages( token );
      }
    } catch ( final Exception e ) {
      LOG.error( e );
      Logs.extreme( ).error( e, e );
      this.allocInfo.abort( );
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        try {
          final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
          VmInstances.terminated( vm );
          VmInstances.terminated( vm );
        } catch ( final Exception e1 ) {
          LOG.error( e1 );
          Logs.extreme( ).error( e1, e1 );
        }
      }
    }
  }
  
  private void setupVolumeMessages( ) throws NoSuchElementException, MetadataException, ExecutionException {
    if ( this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {
      final ServiceConfiguration sc = Topology.lookup( Storage.class, this.cluster.getConfiguration( ).lookupPartition( ) );
      final VirtualBootRecord root = this.allocInfo.getVmTypeInfo( ).lookupRoot( );
      if ( root.isBlockStorage( ) ) {
        final BlockStorageImageInfo imgInfo = ( ( BlockStorageImageInfo ) this.allocInfo.getBootSet( ).getMachine( ) );
        Long volSizeBytes = imgInfo.getImageSizeBytes( );
        Boolean deleteOnTerminate = imgInfo.getDeleteOnTerminate( );
        for ( final BlockDeviceMappingItemType blockDevMapping : this.allocInfo.getRequest( ).getBlockDeviceMapping( ) ) {
          if ( "root".equals( blockDevMapping.getVirtualName( ) ) && ( blockDevMapping.getEbs( ) != null ) ) {
            deleteOnTerminate |= blockDevMapping.getEbs( ).getDeleteOnTermination( );
            if ( blockDevMapping.getEbs( ).getVolumeSize( ) != null ) {
              volSizeBytes = BYTES_PER_GB * blockDevMapping.getEbs( ).getVolumeSize( );
            }
          }
        }
        final int sizeGb = ( int ) Math.ceil( volSizeBytes / BYTES_PER_GB );
        LOG.debug( "About to prepare root volume using bootable block storage: " + imgInfo + " and vbr: " + root );
        for ( final ResourceToken token : this.allocInfo.getAllocationTokens( ) ) {
          final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
          Volume vol = null;
          if ( !vm.getBootRecord( ).hasPersistentVolumes( ) ) {
            vol = Volumes.createStorageVolume( sc, this.allocInfo.getOwnerFullName( ), imgInfo.getSnapshotId( ), sizeGb, this.allocInfo.getRequest( ) );
            if ( deleteOnTerminate ) {
              vm.addPersistentVolume( "/dev/sda1", vol );
            } else {
              vm.addPermanentVolume( "/dev/sda1", vol );
            }
            token.setRootVolume( vol );
          } else {
            final VmVolumeAttachment volumeAttachment = vm.getBootRecord( ).getPersistentVolumes( ).iterator( ).next( );
            vol = Volumes.lookup( null, volumeAttachment.getVolumeId( ) );
            token.setRootVolume( vol );
          }
        }
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private void setupNetworkMessages( ) throws NotEnoughResourcesException {
    final NetworkGroup net = this.allocInfo.getPrimaryNetwork( );
    if ( net != null ) {
      final Request callback = AsyncRequests.newRequest( new StartNetworkCallback( this.allocInfo.getExtantNetwork( ) ) );
      this.messages.addRequest( State.CREATE_NETWORK, callback );
      LOG.debug( "Queued StartNetwork: " + callback );
      EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, callback.getClass( ).getSimpleName( ), net.toString( ) ).debug( );
    }
  }
  
  private void setupVmMessages( final ResourceToken token ) throws Exception {
    final String networkName = NetworkGroups.networkingConfiguration( ).hasNetworking( )
                                                                                        ? this.allocInfo.getPrimaryNetwork( ).getNaturalId( )
                                                                                        : NetworkGroups.lookup(
                                                                                          this.allocInfo.getOwnerFullName( ).asAccountFullName( ),
                                                                                          NetworkGroups.defaultNetworkName( ) ).getNaturalId( );
    
    final SshKeyPair keyInfo = this.allocInfo.getSshKeyPair( );
    final VmTypeInfo vmInfo = this.allocInfo.getVmTypeInfo( );
    Request cb = null;
    try {
      final VirtualBootRecord root = vmInfo.lookupRoot( );
      final VmTypeInfo childVmInfo = this.makeVmTypeInfo( vmInfo, token, root );
      cb = this.makeRunRequest( token, childVmInfo, networkName );
      this.messages.addRequest( State.CREATE_VMS, cb );
      LOG.debug( "Queued RunInstances: " + token );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  private VmTypeInfo makeVmTypeInfo( final VmTypeInfo vmInfo, final ResourceToken token, final VirtualBootRecord root ) throws Exception {
    VmTypeInfo childVmInfo = vmInfo;
    if ( root.isBlockStorage( ) ) {
      childVmInfo = vmInfo.child( );
      final Volume vol = token.getRootVolume( );
      final ServiceConfiguration scConfig = waitForVolume( vol );
      
      VirtualBootRecord vbrRootDevice = childVmInfo.lookupRoot( );
      String volumeId = vol.getDisplayName( );
      String remoteDeviceString = null;
      try {
        AttachStorageVolumeType attachMsg = new AttachStorageVolumeType( Nodes.lookupIqns( this.cluster.getConfiguration( ) ), volumeId );
        final AttachStorageVolumeResponseType scAttachResponse = AsyncRequests.sendSync( scConfig, attachMsg );
        LOG.debug( scAttachResponse );
        remoteDeviceString = scAttachResponse.getRemoteDeviceString( );
        if ( remoteDeviceString == null ) {
          throw new EucalyptusCloudException( "Failed to get remote device string for " + volumeId + " while running instance " + token.getInstanceId( ) );
        } else {
          vbrRootDevice.setResourceLocation( remoteDeviceString );
          final String updateRemoteDevString = remoteDeviceString;
          final Function<String, VmInstance> updateInstance = new Function<String, VmInstance>( ) {
            public VmInstance apply( final String input ) {
              VmVolumeAttachment attachment = VmInstances.lookupVolumeAttachment( input );
              attachment.setRemoteDevice( updateRemoteDevString );
              return attachment.getVmInstance( );
            }
          };
          try {
            Entities.asTransaction( VmInstance.class, updateInstance ).apply( volumeId );
          } catch ( Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        throw ex;
      }
    }//TODO:GRZE:OMGFIXME: move this for bfe to later stage.
    return childVmInfo;
  }
  
  public ServiceConfiguration waitForVolume( final Volume vol ) throws Exception {
    final ServiceConfiguration scConfig = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
    long startTime = System.currentTimeMillis( );
    int numDescVolError = 0;
    while ( ( System.currentTimeMillis( ) - startTime ) < VmInstances.EBS_VOLUME_CREATION_TIMEOUT * 60 * 1000L ) {
      try {
        DescribeStorageVolumesResponseType volState = null;
        try {
          final DescribeStorageVolumesType describeMsg = new DescribeStorageVolumesType( Lists.newArrayList( vol.getDisplayName( ) ) );
          volState = AsyncRequests.sendSync( scConfig, describeMsg );
        } catch ( final Exception e ) {
          if ( numDescVolError++ < 5 ) {
            try {
              TimeUnit.SECONDS.sleep( 5 );
            } catch ( final InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
            }
            continue;
          } else {
            throw e;
          }
        }
        StorageVolume storageVolume = volState.getVolumeSet( ).get( 0 );
        LOG.debug( "Got storage volume info: " + storageVolume );
        if ( "available".equals( storageVolume.getStatus( ) ) ) {
          return scConfig;
        } else if ( "failed".equals( storageVolume.getStatus( ) ) ) {
          throw new EucalyptusCloudException( "volume creation failed" );
        } else {
          TimeUnit.SECONDS.sleep( 5 );
        }
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    throw new EucalyptusCloudException( "volume " + vol.getDisplayName( ) + " was not created in time" );
  }
  
  private Request makeRunRequest( final ResourceToken childToken, final VmTypeInfo vmInfo, final String networkName ) {
    final SshKeyPair keyPair = this.allocInfo.getSshKeyPair( );
    final VmKeyInfo vmKeyInfo = new VmKeyInfo( keyPair.getName( ), keyPair.getPublicKey( ), keyPair.getFingerPrint( ) );
    final String platform = this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ) != null
                                                                                                     ? this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( )
                                                                                                     : "linux"; // ASAP:FIXME:GRZE
    ExtantNetwork exNet;
    try {
      exNet = this.allocInfo.getExtantNetwork( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      exNet = ExtantNetwork.bogus( this.allocInfo.getPrimaryNetwork( ) );
    }
//TODO:GRZE:FINISH THIS.    Date date = Contexts.lookup( ).getContracts( ).get( Contract.Type.EXPIRATION ); 
    final VmRunType run = VmRunType.builder( )
                                   .instanceId( childToken.getInstanceId( ) )
                                   .naturalId( childToken.getInstanceUuid( ) )
                                   .keyInfo( vmKeyInfo )
                                   .launchIndex( childToken.getLaunchIndex( ) )
                                   .networkIndex( childToken.getNetworkIndex( ).getIndex( ) )
                                   .networkNames( this.allocInfo.getNetworkGroups( ) )
                                   .platform( platform )
                                   .reservationId( childToken.getAllocationInfo( ).getReservationId( ) )
                                   .userData( this.allocInfo.getRequest( ).getUserData( ) )
                                   .vlan( exNet.getTag( ) )
                                   .vmTypeInfo( vmInfo )
                                   .owner( this.allocInfo.getOwnerFullName( ) )
                                   .create( );
    final Request<VmRunType, VmRunResponseType> req = AsyncRequests.newRequest( new VmRunCallback( run, childToken ) );
    return req;
  }
  
  @Override
  public void run( ) {
    this.messages.run( );
  }
  
}
