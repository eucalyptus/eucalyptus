/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
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

package com.eucalyptus.blockstorage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientUnauthorizedComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.backend.AttachVolumeResponseType;
import com.eucalyptus.compute.common.backend.AttachVolumeType;
import com.eucalyptus.compute.common.backend.CreateVolumeResponseType;
import com.eucalyptus.compute.common.backend.CreateVolumeType;
import com.eucalyptus.compute.common.backend.DeleteVolumeResponseType;
import com.eucalyptus.compute.common.backend.DeleteVolumeType;
import com.eucalyptus.compute.common.backend.DetachVolumeResponseType;
import com.eucalyptus.compute.common.backend.DetachVolumeType;
import com.eucalyptus.compute.common.backend.EnableVolumeIOResponseType;
import com.eucalyptus.compute.common.backend.EnableVolumeIOType;
import com.eucalyptus.compute.common.backend.ModifyVolumeAttributeResponseType;
import com.eucalyptus.compute.common.backend.ModifyVolumeAttributeType;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.tags.TagHelper;
import com.google.common.base.Predicates;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.cluster.callback.VolumeAttachCallback;
import com.eucalyptus.cluster.callback.VolumeDetachCallback;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.compute.common.internal.vm.MigrationState;
import com.eucalyptus.compute.common.internal.vm.VmEphemeralAttachment;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment.AttachmentState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

import edu.ucsb.eucalyptus.cloud.VolumeSizeExceededException;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeType;

@ComponentNamed("computeVolumeManager")
public class VolumeManager {
  private static final int VOL_CREATE_RETRIES = 10;
  
  private static Logger    LOG                = Logger.getLogger( VolumeManager.class );

  public CreateVolumeResponseType CreateVolume( final CreateVolumeType request ) throws EucalyptusCloudException {
    Context ctx = Contexts.lookup( );
    Long volSize = request.getSize( ) != null
                                             ? Long.parseLong( request.getSize( ) )
                                             : null;
    final String snapId = normalizeOptionalSnapshotIdentifier( request.getSnapshotId() );
    Integer snapSize = 0;
    String partition = request.getAvailabilityZone( );
    
    if ( ( request.getSnapshotId( ) == null && request.getSize( ) == null ) ) {
      throw new EucalyptusCloudException( "One of size or snapshotId is required as a parameter." );
    }

    try {
      TagHelper.validateTagSpecifications( request.getTagSpecification( ) );
    } catch ( MetadataException e ) {
      throw new ClientComputeException( "InvalidParameterValue", e.getMessage( ) );
    }
    final List<ResourceTag> volumeTags = TagHelper.tagsForResource( request.getTagSpecification( ), PolicySpec.EC2_RESOURCE_VOLUME );

    if ( !volumeTags.isEmpty( ) ) {
      if ( !TagHelper.createTagsAuthorized( ctx, PolicySpec.EC2_RESOURCE_VOLUME ) ) {
        throw new ClientUnauthorizedComputeException( "Not authorized to create tags by " + ctx.getUser( ).getName( ) );
      }
    }
    if ( snapId != null ) {
      try {
        Snapshot snap = Transactions.find( Snapshot.named( null, normalizeOptionalSnapshotIdentifier( snapId ) ) );
        snapSize = snap.getVolumeSize( );
        if ( !Predicates.and( Snapshots.FilterPermissions.INSTANCE, RestrictedTypes.filterPrivilegedWithoutOwner()).apply(snap)) {
          throw new ClientUnauthorizedComputeException( "Not authorized to use snapshot " + snapId + " by " + ctx.getUser( ).getName( ) );
        }
        // Volume created from a snapshot cannot be smaller than the size of the snapshot
        if (volSize != null && snap != null && snap.getVolumeSize() != null && volSize < snap.getVolumeSize()) {
          throw new EucalyptusCloudException( "Volume size cannot be less than snapshot size" );
        }
      } catch ( NoSuchElementException ex ) {
          throw new ClientComputeException( "InvalidSnapshot.NotFound", "The snapshot " + snapId + " does not exist.");
      } catch ( TransactionException ex ) {
          throw Exceptions.toUndeclared( "Creating volume failed:  Contact the administrator to report the problem.", ex );
      }
    }
    final Integer sizeFromRequest = request.getSize() != null  
        ? new Integer(request.getSize()) 
        : null;
    if ( sizeFromRequest != null && sizeFromRequest <= 0) {
        throw new EucalyptusCloudException( "Failed to create volume because the parameter size (" + sizeFromRequest + ") must be greater than zero.");
    }
    final Integer newSize = sizeFromRequest != null 
        ? sizeFromRequest 
        : (snapId != null 
        ? snapSize : new Integer(-1));
    Exception lastEx = null;
    for ( int i = 0; i < VOL_CREATE_RETRIES; i++ ) {
      try {
        final ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( partition ) );
        final String arn = Accounts.getAuthenticatedArn( ctx.getUser( ) );
        final UserFullName owner = ctx.getUserFullName( );
        Function<Long, Volume> allocator = new Function<Long, Volume>( ) {
          @Override
          public Volume apply( Long size ) {
            try {
              return Volumes.createStorageVolume( sc, arn, owner, snapId, Ints.checkedCast( size ),
                  volume -> TagHelper.createOrUpdateTags( owner, volume, volumeTags ) );
            } catch ( ExecutionException ex ) {
              throw Exceptions.toUndeclared( ex );
            }
          }
        };
        Volume newVol = RestrictedTypes.allocateMeasurableResource(
            newSize.longValue( ),
            allocator,
            Volume.exampleResource( owner, snapId, partition, newSize ) );
        CreateVolumeResponseType reply = request.getReply( );
        reply.setVolume( newVol.morph( new com.eucalyptus.compute.common.Volume( ) ) );
        Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( Volume.class ).getResourceTagMap(
            AccountFullName.getInstance( ctx.getAccountNumber( ) ),
            Collections.singleton( newVol.getDisplayName( ) ) );
        Tags.addFromTags( reply.getVolume().getTagSet(), ResourceTag.class, tagsMap.get( newVol.getDisplayName( ) ) );
        return reply;
      } catch ( RuntimeException ex ) {
        if ( Exceptions.isCausedBy( ex, NoSuchElementException.class ) ) {
          throw new ClientComputeException( "InvalidZone.NotFound", "The zone '"+partition+"' does not exist." );
        }
        if ( !( ex.getCause( ) instanceof ExecutionException ) ) {
          throw handleException( ex );
        } else {
          LOG.error( ex, ex );
          lastEx = ex;
        }
      } catch ( AuthException e ) {
        throw handleException( e );
      }
    }
    throw new EucalyptusCloudException( "Failed to create volume after " + VOL_CREATE_RETRIES + " because of: " + lastEx, lastEx );
  }
  
  public DeleteVolumeResponseType DeleteVolume( final DeleteVolumeType request ) throws EucalyptusCloudException {
    DeleteVolumeResponseType reply = ( DeleteVolumeResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    reply.set_return( false );
    final Function<String, Volume> deleteVolume = new Function<String, Volume>( ) {
      @Override
      public Volume apply( final String input ) {
        try {
          Volume vol = null;
          try {
            vol = Entities.uniqueResult( Volume.named( ctx.getUserFullName( ).asAccountFullName( ), input ) );
          } catch ( NoSuchElementException e ) {
            // Slow path: try searching globally
            try {
              vol = Entities.uniqueResult( Volume.named( null, input ) );
            } catch ( NoSuchElementException e2 ) {
              throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVolume.NotFound", "The volume '"+input+"' does not exist" ) );
            }
          }
          if ( !RestrictedTypes.filterPrivileged( ).apply( vol ) ) {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete volume by " + ctx.getUser( ).getName( ) ) );
          }
          try {
            VmVolumeAttachment attachment = VmInstances.lookupVolumeAttachment( input );
            throw Exceptions.toUndeclared( new ClientComputeException( "VolumeInUse", "Volume is currently attached to: " + attachment.getVmInstance( ).getDisplayName( ) ) );
          } catch ( NoSuchElementException ex ) {
            /** no such volume attached, move along... **/
          }
          if ( State.FAIL.equals( vol.getState( ) ) || State.ANNIHILATED.equals( vol.getState( ) ) ) {
            Entities.delete( vol );
            return vol;
          } else {
            try {
              ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
              DeleteStorageVolumeResponseType scReply = AsyncRequests.sendSync( sc, new DeleteStorageVolumeType( vol.getDisplayName( ) ) );
              if ( scReply.get_return( ) ) {
                Volumes.annihilateStorageVolume( vol );
                return vol;
              } else {
                throw Exceptions.toUndeclared( "Storage Controller returned false:  Contact the administrator to report the problem." );
              }
            } catch ( Exception ex ) {
              throw Exceptions.toUndeclared( "Delete volume request failed because of: " + ex.getMessage(), ex);
            }
          }
        } catch ( NoSuchElementException ex ) {
          throw ex;
        } catch ( TransactionException ex ) {
          throw Exceptions.toUndeclared( "Deleting volume failed:  Contact the administrator to report the problem.", ex );
        }
      }
    };
    try {
      Entities.asTransaction( Volume.class, deleteVolume ).apply( normalizeVolumeIdentifier( request.getVolumeId() ) );
      reply.set_return( true );
      return reply;
    } catch ( NoSuchElementException ex ) {
      return reply;
    } catch ( RuntimeException ex ) {
      Exceptions.rethrow( ex, ComputeException.class );
      throw ex;
    }
  }

  public EnableVolumeIOResponseType EnableVolumeIO( EnableVolumeIOType request ) {
    return request.getReply( );
  }

  public AttachVolumeResponseType AttachVolume( AttachVolumeType request ) throws EucalyptusCloudException {
    AttachVolumeResponseType reply = ( AttachVolumeResponseType ) request.getReply( );
    final String deviceName = request.getDevice( );
    final String volumeId = normalizeVolumeIdentifier( request.getVolumeId() );
    final String instanceId = normalizeInstanceIdentifier( request.getInstanceId() );
    final Context ctx = Contexts.lookup( );
    
    if (  deviceName == null || !validateDeviceName( deviceName ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Value (" + deviceName + ") for parameter device is invalid." );
    }
    
    VmInstance vm = null;
    try {
      vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
    } catch ( final NoSuchElementException e ) {
      throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '"+request.getInstanceId()+"' does not exist" );
    } catch ( Exception ex ) {
      throw handleException( ex );
    }
    if ( MigrationState.isMigrating( vm ) ) {
      throw Exceptions.toUndeclared( "Cannot attach a volume to an instance which is currently migrating: "
                                     + vm.getInstanceId( )
                                     + " "
                                     + vm.getRuntimeState().getMigrationTask( ) );
    }

    AccountFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
    Volume volume = null;
    try{
      volume = Volumes.lookup( ownerFullName, volumeId );
    }catch(final NoSuchElementException ex){
      try {
        volume = Volumes.lookup( null, volumeId );
      } catch ( NoSuchElementException e ) {
        throw new ClientComputeException( "InvalidVolume.NotFound", "The volume '"+request.getVolumeId()+"' does not exist" );
      }
    }
    
    if ( !RestrictedTypes.filterPrivileged( ).apply( volume ) ) {
      throw new ClientUnauthorizedComputeException( "Not authorized to attach volume " + volumeId + " by " + ctx.getUser( ).getName( ) );
    }
    // check volumes
    try {
      vm.lookupVolumeAttachmentByDevice( deviceName );
      throw new ClientComputeException( "InvalidParameterValue", "Already have a device attached to: " + request.getDevice( ) );
    } catch ( NoSuchElementException ex1 ) {
      /** no attachment **/
    }
    // check ephemeral devices
    if ( Iterables.any(VmInstances.lookupEphemeralDevices(instanceId),
          new Predicate<VmEphemeralAttachment>() {
            @Override
            public boolean apply(VmEphemeralAttachment device) {
             return deviceName.endsWith(device.getShortDeviceName());
           }
      }) )
        throw new ClientComputeException( "InvalidParameterValue", "Already have an ephemeral device attached to: " + request.getDevice( ) );

    try {
      VmInstances.lookupVolumeAttachment( volumeId );
      throw new ClientComputeException( "VolumeInUse", "Volume already attached: " + volumeId );
    } catch ( NoSuchElementException ex1 ) {
      /** no attachment **/
    }
    
    Partition volPartition = Partitions.lookupByName( volume.getPartition( ) );
    ServiceConfiguration sc = Topology.lookup( Storage.class, volPartition );
    ServiceConfiguration scVm = Topology.lookup( Storage.class, vm.lookupPartition( ) );
    if ( !sc.equals( scVm ) ) {
      throw new EucalyptusCloudException( "Can only attach volumes in the same zone: " + volumeId );
    }
    
    // check if instance is stopped
    if ( VmState.STOPPED.apply( vm ) ) {
      // Volume attachment to an EBS backed instance in stopped state. Don't get attachment token from SC since its a part of instance start up
      // process. Just add the persistent attachment record to the VM

      String rootDevice = vm.getBootRecord().getMachine().getRootDeviceName();
      
      // swathi - assuming delete on terminate flag to always be false. Its better to err on the safe side and not delete volumes
      // add root attachment
      VmInstances.addPersistentVolume( vm, deviceName, volume, rootDevice.equals( deviceName ), false );
    } else if ( VmState.RUNNING.apply( vm ) ) {
      // A normal volume attachment. Get attachment token from SC and fire attachment request to NC/CC
      
      ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition() );
      GetVolumeTokenResponseType scGetTokenResponse;
      try {
        GetVolumeTokenType req = new GetVolumeTokenType(volume.getDisplayName());
        scGetTokenResponse = AsyncRequests.sendSync(sc, req);
      } catch (Exception e) {
        LOG.warn("Failed to attach volume " + volume.getDisplayName(), e);
        throw new EucalyptusCloudException(e.getMessage(), e);
      }

      // The SC should not know the format, so the CLC must construct the special format
      String token = StorageProperties.formatVolumeAttachmentTokenForTransfer(scGetTokenResponse.getToken(), volume.getDisplayName());
      final ClusterAttachVolumeType attachVolume = new ClusterAttachVolumeType();
      attachVolume.setInstanceId(request.getInstanceId());
      attachVolume.setVolumeId(request.getVolumeId());
      attachVolume.setDevice(request.getDevice());
      attachVolume.setRemoteDevice(token);

      // Add volume attachment record to VM
      VmInstances.addTransientVolume( vm, deviceName, token, volume );
      
      // Fire attach volume request to NC/CC
      final VolumeAttachCallback cb = new VolumeAttachCallback(attachVolume);
      AsyncRequests.newRequest(cb).dispatch(ccConfig);
    } else {
      throw new ClientComputeException( "IncorrectState", "Instance '"+instanceId+"' is not 'running'" );
    }
    
    EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_ATTACH )
               .withDetails( volume.getOwner( ).toString( ), volume.getDisplayName( ), "instance", vm.getInstanceId( ) )
               .withDetails( "partition", vm.getPartition( ).toString( ) ).info( );
    reply.setAttachedVolume( new AttachedVolume(volume.getDisplayName(), vm.getInstanceId(), request.getDevice()) );
    
    Volumes.fireUsageEvent(volume, VolumeEvent.forVolumeAttach(vm.getInstanceUuid(), volume.getDisplayName()));
    return reply;
  }
  
  public DetachVolumeResponseType detach( DetachVolumeType request ) throws EucalyptusCloudException {
    final DetachVolumeResponseType reply = ( DetachVolumeResponseType ) request.getReply( );
    final String volumeId = normalizeVolumeIdentifier( request.getVolumeId( ) );
    final String instanceId = normalizeOptionalInstanceIdentifier( request.getInstanceId( ) );
    final Context ctx = Contexts.lookup( );
    
    Volume vol;
    try {
      vol = Volumes.lookup( ctx.getUserFullName( ).asAccountFullName( ), volumeId );
    } catch ( NoSuchElementException ex ) {
      try {
        vol = Volumes.lookup(null, volumeId);
      } catch ( NoSuchElementException e ) {
        throw new ClientComputeException( "InvalidVolume.NotFound", "The volume '"+request.getVolumeId()+"' does not exist" );
      } 
    }
    if ( !RestrictedTypes.filterPrivileged( ).apply( vol ) ) {
      throw new ClientUnauthorizedComputeException( "Not authorized to detach volume " + volumeId + " by " + ctx.getUser( ).getName( ) );
    }
    
    VmInstance vm = null;
    String remoteDevice = null;
    AttachedVolume volume = null;
    VmVolumeAttachment vmVolAttach = null;
    
    try {
      // Lookup both persistent and transient volumes
      vmVolAttach = VmInstances.lookupVolumeAttachment( volumeId );
      remoteDevice = vmVolAttach.getRemoteDevice( );
      volume = VmVolumeAttachment.asAttachedVolume( vmVolAttach.getVmInstance( ) ).apply( vmVolAttach );
      vm = vmVolAttach.getVmInstance( );
    } catch ( NoSuchElementException ex ) {
      throw new ClientComputeException( "IncorrectState", "Volume is not attached: " + volumeId );
    }
    
    // Cannot detach root volume of EBS backed instance when the instance is not stopped
    if (vmVolAttach.getIsRootDevice() && !VmState.STOPPED.equals(vm.getState())) {
      throw new ClientComputeException("IncorrectState", "Cannot detach root volume " + volumeId + " from EBS backed instance " + vm.getInstanceId()
          + " when instance state is " + vm.getState().toString().toLowerCase());
    }
    
    // Dropping the validation check for device string retrieved from database - EUCA-8330
    if ( vm != null && MigrationState.isMigrating( vm ) ) {
      throw Exceptions.toUndeclared( "Cannot detach a volume from an instance which is currently migrating: "
                                     + vm.getInstanceId( )
                                     + " "
                                     + vm.getRuntimeState().getMigrationTask( ) );
    }
    if ( volume == null ) {
      throw new ClientComputeException( "IncorrectState", "Volume is not attached: " + volumeId );
    }
    if ( !RestrictedTypes.filterPrivileged( ).apply( vm ) ) {
      throw new ClientUnauthorizedComputeException( "Not authorized to detach volume from instance " + instanceId + " by " + ctx.getUser( ).getName( ) );
    }
    if ( instanceId != null && vm != null && !vm.getInstanceId( ).equals( instanceId ) ) {
      throw new ClientComputeException( "InvalidAttachment.NotFound", "Volume is not attached to instance: " + instanceId );
    }
    if ( request.getDevice( ) != null && !request.getDevice( ).equals( "" ) && !volume.getDevice( ).equals( request.getDevice( ) ) ) {
      throw new EucalyptusCloudException( "Volume is not attached to device: " + request.getDevice( ) );
    }
    ServiceConfiguration scVm;
    try {
      scVm = Topology.lookup( Storage.class, vm.lookupPartition( ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( "Failed to lookup SC for partition: " + vm.getPartition( ), ex );
    }
    if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
    	//Ensure that the volume is not attached
    	try {
    	  final DetachStorageVolumeType detach = new DetachStorageVolumeType( volume.getVolumeId( ));
    		AsyncRequests.sendSync( scVm, detach);
    	} catch ( Exception e ) {
    		LOG.debug( e );
    		Logs.extreme( ).debug( e, e );
    		//GRZE: attach is idempotent, failure here is ok, throw new EucalyptusCloudException( e.getMessage( ) );
    	}
    	VmInstances.removeVolumeAttachment( vm, volume.getVolumeId( ) );
    } else {
    	ServiceConfiguration ccConfig = null;
    	try {
    		ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
    	} catch ( NoSuchElementException e ) {
    		LOG.debug( e, e );
    		throw new EucalyptusCloudException( "Cluster does not exist in partition: " + vm.getPartition( ) );
    	}
      final ClusterDetachVolumeType detachVolume = new ClusterDetachVolumeType( );
      detachVolume.setVolumeId( volume.getVolumeId( ) );
      detachVolume.setRemoteDevice( remoteDevice );
      detachVolume.setDevice( volume.getDevice( ).replaceAll( "unknown,requested:", "" ) );
      detachVolume.setInstanceId( vm.getInstanceId( ) );
      detachVolume.setForce( request.getForce( ) );
    	VolumeDetachCallback ncDetach = new VolumeDetachCallback( detachVolume );
    	/* No SC action required, send directly to NC
    	 * try {
    		AsyncRequests.sendSync( scVm, new DetachStorageVolumeType( volume.getVolumeId( ) ) );
    	} catch ( Exception e ) {
    		LOG.debug( e );
    		Logs.extreme( ).debug( e, e );
    		//GRZE: attach is idempotent, failure here is ok, throw new EucalyptusCloudException( e.getMessage( ) );
    	}*/
    	AsyncRequests.newRequest( ncDetach ).dispatch( ccConfig );
    	
    	//Update the state of the attachment to 'detaching'
      VmInstances.updateVolumeAttachment( vm, volumeId, AttachmentState.detaching );
      volume.setStatus( "detaching" );
    }
    
    reply.setDetachedVolume( volume );
    Volumes.fireUsageEvent(vol, VolumeEvent.forVolumeDetach(vm.getInstanceUuid(), vm.getInstanceId()));
    return reply;
  }

  public ModifyVolumeAttributeResponseType modifyVolumeAttribute( ModifyVolumeAttributeType request ) {
    return request.getReply();
  }

  private boolean validateDeviceName(String DeviceName){
    return java.util.regex.Pattern.matches("^[a-zA-Z\\d/]{3,10}$", DeviceName);
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ClientComputeException {
    try {
      return Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidParameterValue", String.format( message, e.getIdentifier( ) ) );
    }
  }

  @Nullable
  private static String normalizeOptionalSnapshotIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier,  "snap", false, "Value (%s) for parameter snapshotId is invalid. Expected: 'snap-...'." );    
  }

  @Nullable
  private static String normalizeOptionalInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, VmInstance.ID_PREFIX, false, "Value (%s) for parameter instanceId is invalid. Expected: 'i-...'." );
  }

  private static String normalizeInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier( 
        identifier, VmInstance.ID_PREFIX, true, "Value (%s) for parameter instanceId is invalid. Expected: 'i-...'." );
  }

  private static String normalizeVolumeIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, Volume.ID_PREFIX, true, "Value (%s) for parameter volume is invalid. Expected: 'vol-...'." );
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static ComputeException handleException( final Exception e ) throws ComputeException {
    final ComputeException cause = Exceptions.findCause( e, ComputeException.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthException authException = Exceptions.findCause( e, AuthException.class );
    if ( authException != null ) {
      if ( authException instanceof AuthQuotaException ) {
        throw new ClientComputeException( "VolumeLimitExceeded", authException.getMessage( ) );
      } else {
        throw new ClientUnauthorizedComputeException( authException.getMessage( ) );
      }
    }

    final VolumeSizeExceededException volumeSizeException = 
        Exceptions.findCause( e, VolumeSizeExceededException.class );
    if ( volumeSizeException != null )
      throw new ClientComputeException(
          "VolumeLimitExceeded", volumeSizeException.getMessage( ) );
 
    LOG.error( e, e );

    final ComputeException exception = new ComputeException( "InternalError", String.valueOf( e.getMessage( ) ) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
