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

package com.eucalyptus.cloud.run;

import static com.eucalyptus.images.Images.findEbsRootOptionalSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.callback.ResourceStateCallback;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Transactions;
import com.google.common.base.Function;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.Snapshots;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.VmRunType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.ResourceState;
import com.eucalyptus.cluster.callback.StartNetworkCallback;
import com.eucalyptus.cluster.callback.VmRunCallback;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.keys.SshKeyPair;
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
import com.eucalyptus.vm.VmEphemeralAttachment;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmVolumeAttachment;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.concurrent.TimeUnit;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

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
    UPDATE_RESOURCES,
    ATTACH_VOLS,
    ASSIGN_ADDRESSES,
    FINISHED,
    ROLLBACK,
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
        if ( EventRecord.isDebugEnabled( ClusterAllocator.class ) ) {
          EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, LogUtil.dumpObject( allocInfo ) ).debug( );
        }
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
    
  }
  
  public static Predicate<Allocation> get( ) {
    return SubmitAllocation.INSTANCE;
  }
  
  private ClusterAllocator( final Allocation allocInfo ) {
    this.allocInfo = allocInfo;
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      this.cluster = Clusters.lookup( Topology.lookup( ClusterController.class, allocInfo.getPartition( ) ) );
      this.messages = new StatefulMessageSet<State>( this.cluster, State.values( ) );
      this.setupNetworkMessages( );
      this.setupVolumeMessages();
      this.updateResourceMessages( );
      db.commit( );
    } catch ( final Exception e ) {
      db.rollback( );
      cleanupOnFailure( allocInfo, e );
      return;
    }
    
    try {
      for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
        this.setupVmMessages( token );
      }
    } catch ( final Exception e ) {
      cleanupOnFailure( allocInfo, e );
    }
  }

  private void updateResourceMessages() {
    /**
     * NOTE: Here we need to do another resource refresh.
     * After an unordered instance type is run it is uncertain what the current resource
     * availability is.
     * This is the point at which the backing cluster would correctly respond w/ updated resource
     * counts.
     */
    Set<Cluster> clustersToUpdate = Sets.newHashSet();
    for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
      if ( token.isUnorderedType() ) {
        clustersToUpdate.add( token.getCluster() );
      }
    }
    for ( final Cluster cluster : clustersToUpdate ) {
      ResourceStateCallback cb = new ResourceStateCallback();
      cb.setSubject( cluster );
      Request<DescribeResourcesType,DescribeResourcesResponseType> request = AsyncRequests.newRequest( cb );
      this.messages.addRequest( State.UPDATE_RESOURCES, request );
    }
  }

  private void cleanupOnFailure( final Allocation allocInfo, final Exception e ) {
    LOG.error( e );
    Logs.extreme().error( e, e );
    this.allocInfo.abort( );
    for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
      try {
        final VmInstance vm = VmInstances.lookup( token.getInstanceId() );
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
  }

  // Modifying the logic to enable multiple block device mappings for boot from ebs. Fixes EUCA-3254 and implements EUCA-4786
  private void setupVolumeMessages( ) throws NoSuchElementException, MetadataException, ExecutionException {
    
    if (  this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo  ) {
      List<BlockDeviceMappingItemType> instanceDeviceMappings = new ArrayList<BlockDeviceMappingItemType>(this.allocInfo.getRequest().getBlockDeviceMapping());
      final ServiceConfiguration sc = Topology.lookup( Storage.class, this.cluster.getConfiguration( ).lookupPartition( ) );
      
      final BlockStorageImageInfo imgInfo = ( ( BlockStorageImageInfo ) this.allocInfo.getBootSet( ).getMachine( ) );   	                
      final String rootDevName = imgInfo.getRootDeviceName();
      Long volSizeBytes = imgInfo.getImageSizeBytes( );

      // Find out the root volume size so that device mappings that don't have a size or snapshot ID can use the root volume size
      for ( final BlockDeviceMappingItemType blockDevMapping : Iterables.filter( instanceDeviceMappings, findEbsRootOptionalSnapshot(rootDevName) ) ) {
        if ( blockDevMapping.getEbs( ).getVolumeSize( ) != null ) {
          volSizeBytes = BYTES_PER_GB * blockDevMapping.getEbs( ).getVolumeSize( );
        }
      } 

      int rootVolSizeInGb = ( int ) Math.ceil( ( ( double ) volSizeBytes ) / BYTES_PER_GB );

      for ( final ResourceToken token : this.allocInfo.getAllocationTokens( ) ) {
        final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
        if ( !vm.getBootRecord( ).hasPersistentVolumes( ) ) { // First time a bfebs instance starts up, there are no persistent volumes
          
          for (final BlockDeviceMappingItemType mapping : instanceDeviceMappings) {
            if( Images.isEbsMapping( mapping ) ) {
              LOG.debug("About to prepare volume for instance " + vm.getDisplayName() + " to be mapped to " + mapping.getDeviceName() + " device");
              
              //spark - EUCA-7800: should explicitly set the volume size
              int volumeSize = mapping.getEbs().getVolumeSize()!=null? mapping.getEbs().getVolumeSize() : -1;
              if(volumeSize<=0){
                if(mapping.getEbs().getSnapshotId() != null){
                  final Snapshot originalSnapshot = Snapshots.lookup(null, mapping.getEbs().getSnapshotId() );
                  volumeSize = originalSnapshot.getVolumeSize();
                }else
                  volumeSize = rootVolSizeInGb;
              }
              final UserFullName fullName = this.allocInfo.getOwnerFullName();
              final String snapshotId = mapping.getEbs().getSnapshotId();
              final int volSize = volumeSize;
              final BaseMessage request = this.allocInfo.getRequest();
              final Callable<Volume> createVolume = new Callable<Volume>( ) {
                  public Volume call( ) throws Exception {
                    return Volumes.createStorageVolume(sc, fullName, snapshotId, volSize, request);
                  }
              };

              final Volume volume; // allocate in separate transaction to ensure metadata matches back-end
              try {
                volume = Threads.enqueue( Eucalyptus.class, ClusterAllocator.class, createVolume ).get( );
              } catch ( InterruptedException e ) {
                throw Exceptions.toUndeclared( "Interrupted when creating volume from snapshot.", e );
              }

              final Boolean isRootDevice = mapping.getDeviceName().equals(rootDevName);
              if ( mapping.getEbs().getDeleteOnTermination() ) {
                vm.addPersistentVolume( mapping.getDeviceName(), volume, isRootDevice );
              } else {
                vm.addPermanentVolume( mapping.getDeviceName(), volume, isRootDevice );
              }

              // Populate all volumes into resource token so they can be used for attach ops and vbr construction
              if( isRootDevice ) {
                token.setRootVolume( volume );
              } else {
                token.getEbsVolumes().put(mapping.getDeviceName(), volume);
              }
            } else if ( mapping.getVirtualName() != null ) {
              vm.addEphemeralAttachment(mapping.getDeviceName(), mapping.getVirtualName());
              // Populate all ephemeral devices into resource token so they can used for vbr construction
              token.getEphemeralDisks().put(mapping.getDeviceName(), mapping.getVirtualName());
            }
          }
        } else { // This block is hit when starting a stopped bfebs instance
          // Although volume attachment records exist and the volumes are marked attached, all volumes are in detached state when the instance is stopped. 
          // Go through all volume attachments and populate them into the resource token so they can be used for attach ops and vbr construction
          boolean foundRoot = false;
          for (VmVolumeAttachment attachment : vm.getBootRecord( ).getPersistentVolumes( )) {
        	final Volume volume = Volumes.lookup( null, attachment.getVolumeId( ) );
            if (attachment.getIsRootDevice() || attachment.getDevice().equals(rootDevName) ) {
              token.setRootVolume( volume );
              foundRoot = true;
            } else {
              token.getEbsVolumes().put(attachment.getDevice(), volume);
            }
          }
          
          // Root volume may have been detached. In that case throw an error and exit
          if ( !foundRoot ) {
            LOG.error("No volume attachment found for root device. Attach a volume to root device and retry");
            throw new MetadataException("No volume attachment found for root device. Attach a volume to root device and retry");
          }
          
          // Go through all ephemeral attachment records and populate them into resource token so they can used for vbr construction
          for (VmEphemeralAttachment attachment : vm.getBootRecord( ).getEphmeralStorage()) {
            token.getEphemeralDisks().put(attachment.getDevice(), attachment.getEphemeralId());
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
    try {
      final VmTypeInfo childVmInfo = this.makeVmTypeInfo( vmInfo, token );
      final VmRunCallback callback = this.makeRunCallback( token, childVmInfo, networkName );
      final Request<VmRunType, VmRunResponseType> req = AsyncRequests.newRequest( callback );
      this.messages.addRequest( State.CREATE_VMS, req );
      this.messages.addCleanup( new Runnable( ) {
        @Override
        public void run( ) {
          if ( token.isPending( ) ) try {
            token.release( );
          } catch ( final ResourceState.NoSuchTokenException e ) {
            Logs.extreme( ).error( e, e );
          }
        }
      } );
      LOG.debug( "Queued RunInstances: " + token );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  // Modifying the logic to enable multiple block device mappings for boot from ebs. Fixes EUCA-3254 and implements EUCA-4786
  // Using resource token to construct vbr record rather than volume attachments from the database as there might be race condition
  // where the vm instance record may not have been updated with the volume attachments. EUCA-5670
  private VmTypeInfo makeVmTypeInfo( final VmTypeInfo vmInfo, final ResourceToken token ) throws Exception {
    VmTypeInfo childVmInfo = vmInfo.child( );
    
    if ( this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {        
    	String instanceId = token.getInstanceId();
    	
    	// Deal with the root volume first
    	VirtualBootRecord rootVbr = childVmInfo.lookupRoot();
    	Volume rootVolume = token.getRootVolume();
    	String volumeId = rootVolume.getDisplayName( );
    	String volumeToken = null;
    	
    	// Wait for root volume
    	LOG.debug("Wait for root ebs volume " + rootVolume.getDisplayName() +  " to become available");
    	final ServiceConfiguration scConfig = waitForVolume(rootVolume);
    	
    	// Attach root volume
    	try {
    		LOG.debug("About to get attachment token for volume " + rootVolume.getDisplayName() + " to instance " + instanceId);    		
    		GetVolumeTokenResponseType scGetTokenResponse;
    		try {
    			GetVolumeTokenType req = new GetVolumeTokenType(volumeId);
    			scGetTokenResponse = AsyncRequests.sendSync(scConfig, req);
    		} catch ( Exception e ) {
    			LOG.debug( e, e );
    			throw new EucalyptusCloudException( e.getMessage( ), e );
    		}
    		
    		LOG.debug("Got volume token response from SC for volume " + rootVolume.getDisplayName() + " and instance " + instanceId + "\n" + scGetTokenResponse);
    		volumeToken = scGetTokenResponse.getToken();                
    		if ( volumeToken == null ) {
    			throw new EucalyptusCloudException( "Failed to get remote device string for " + volumeId + " while running instance " + token.getInstanceId( ) );
    		} else {
    			//Do formatting here since formatting is for messaging only.
    			volumeToken = StorageProperties.formatVolumeAttachmentTokenForTransfer(volumeToken, volumeId);
    		}
    		rootVbr.setResourceLocation(volumeToken);
    		rootVbr.setSize(rootVolume.getSize() * BYTES_PER_GB);
    		//vm.updatePersistantVolume(remoteDeviceString, rootVolume); Skipping this step for now as no one seems to be using it
    	} catch (final Exception ex) {
    		LOG.error(ex);
    		Logs.extreme().error(ex, ex);
    		throw ex;
    	}
    	
    	// Deal with the remaining ebs volumes
    	for (Entry<String, Volume> mapping : token.getEbsVolumes().entrySet()) {
    		Volume volume = mapping.getValue();
    		if (volume.getSize() <= 0) {
    			volume = Volumes.lookup(this.allocInfo.getOwnerFullName(), mapping.getValue().getDisplayName());	
    		}
    		volumeId = volume.getDisplayName();  
    		
    		LOG.debug("Wait for volume " + volumeId +  " to become available");
    		final ServiceConfiguration scConfigLocal = waitForVolume(volume);

    		try {
    			LOG.debug("About to get attachment token for volume " + volume.getDisplayName() + " to instance " + instanceId);    		
    			GetVolumeTokenResponseType scGetTokenResponse;
    			try {
    				GetVolumeTokenType req = new GetVolumeTokenType(volumeId);
    				scGetTokenResponse = AsyncRequests.sendSync(scConfigLocal, req);
    			} catch ( Exception e ) {
    				LOG.debug( e, e );
    				throw new EucalyptusCloudException( e.getMessage( ), e );
    			}
    			
    			LOG.debug("Got volume token response from SC for volume " + volume.getDisplayName() + " and instance " + instanceId + "\n" + scGetTokenResponse);
    			volumeToken = scGetTokenResponse.getToken();
    			if ( volumeToken == null ) {
    				throw new EucalyptusCloudException( "Failed to get remote device string for " + volumeId + " while running instance " + token.getInstanceId( ) );
    			} else {
    				//Do formatting here since formatting is for messaging only.
    				volumeToken = StorageProperties.formatVolumeAttachmentTokenForTransfer(volumeToken, volumeId);
    				VirtualBootRecord vbr = new VirtualBootRecord(volumeId, volumeToken, "ebs", mapping.getKey(), (volume.getSize() * BYTES_PER_GB), "none");
    				childVmInfo.getVirtualBootRecord().add(vbr);
    				//vm.updatePersistantVolume(remoteDeviceString, volume); Skipping this step for now as no one seems to be using it
    			}
    		} catch (final Exception ex) {
    			LOG.error(ex);
    			Logs.extreme().error(ex, ex);
    			throw ex;
    		}
    	}
    	// FIXME: multiple ephemerals will result in wrong disk sizes
    	for( String deviceName : token.getEphemeralDisks().keySet()  ) {
    		childVmInfo.setEphemeral( 0, deviceName, (this.allocInfo.getVmType().getDisk( ) * BYTES_PER_GB), "none" );
    	}
    	
    	LOG.debug("Instance information: " + childVmInfo.dump());
    }
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
  
  private VmRunCallback makeRunCallback( final ResourceToken childToken, final VmTypeInfo vmInfo, final String networkName ) {
    final SshKeyPair keyPair = this.allocInfo.getSshKeyPair( );
    final VmKeyInfo vmKeyInfo = new VmKeyInfo( keyPair.getName( ), keyPair.getPublicKey( ), keyPair.getFingerPrint( ) );
    final String platform = this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ) != null
                                                                                                     ? this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( )
                                                                                                     : "linux"; // ASAP:FIXME:GRZE
    
//TODO:GRZE:FINISH THIS.    Date date = Contexts.lookup( ).getContracts( ).get( Contract.Type.EXPIRATION ); 
    final VmRunType run = VmRunType.builder()
                                   .instanceId( childToken.getInstanceId() )
                                   .naturalId( childToken.getInstanceUuid() )
                                   .keyInfo( vmKeyInfo )
                                   .launchIndex( childToken.getLaunchIndex() )
                                   .networkIndex( childToken.getNetworkIndex().getIndex() )
                                   .networkNames( this.allocInfo.getNetworkGroups() )
                                   .platform( platform )
                                   .reservationId( childToken.getAllocationInfo().getReservationId() )
                                   .userData( this.allocInfo.getRequest().getUserData() )
                                   .vlan( childToken.getExtantNetwork( ) != null ? childToken.getExtantNetwork().getTag( ) : new Integer(-1) )
                                   .vmTypeInfo( vmInfo )
                                   .owner( this.allocInfo.getOwnerFullName( ) )
                                   .create( );
    return new VmRunCallback( run, childToken );
  }
  
  @Override
  public void run( ) {
    this.messages.run( );
  }
  
}
