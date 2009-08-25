/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.cluster.VolumeAttachCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VolumeDetachCallback;
import edu.ucsb.eucalyptus.cloud.state.Snapshot;
import edu.ucsb.eucalyptus.cloud.state.State;
import edu.ucsb.eucalyptus.cloud.state.Volume;
import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeType;
import edu.ucsb.eucalyptus.msgs.StorageVolume;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class VolumeManager {
  static String PERSISTENCE_CONTEXT = "eucalyptus_volumes";

  private static String ID_PREFIX = "vol";
  private static Logger LOG = Logger.getLogger( VolumeManager.class );

//  static {
//    Clusters.getInstance().register( new Cluster( new ClusterInfo( "bogocluster", "lollerskates", 8774 ) ) );
//  }

  public static EntityWrapper<Volume> getEntityWrapper() {
    return new EntityWrapper<Volume>( PERSISTENCE_CONTEXT );
  }

  public CreateVolumeResponseType CreateVolume( CreateVolumeType request ) throws EucalyptusCloudException {
    if ( !Clusters.getInstance().contains( request.getAvailabilityZone() ) ) {
      throw new EucalyptusCloudException( "Zone does not exist: " + request.getAvailabilityZone() );
    }
    if( ( request.getSnapshotId() == null && request.getSize() == null ) || ( request.getSnapshotId() != null && request.getSize() != null ) ) {
      throw new EucalyptusCloudException( "One of 'snapshotId' or 'size' must be set." );
    }

    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper();
    if ( !( request.getSnapshotId() == null ) ) {
      String userName = request.isAdministrator() ? null : request.getUserId();
      try {
        db.recast( Snapshot.class ).getUnique( Snapshot.named( userName, request.getSnapshotId() ) );
      } catch ( EucalyptusCloudException e ) {
        LOG.debug( e, e );
        db.rollback();
        throw new EucalyptusCloudException( "Snapshot does not exist: " + request.getSnapshotId() );
      }
    }
    String newId = null;
    Volume newVol = null;
    while ( true ) {
      newId = Hashes.generateId( request.getUserId(), ID_PREFIX );
      try {
        db.getUnique( Volume.ownedBy( newId ) );
      } catch ( EucalyptusCloudException e ) {
        newVol = new Volume( request.getUserId(), newId, new Integer( request.getSize() != null ? request.getSize() : "-1" ),
                             request.getAvailabilityZone(), request.getSnapshotId() );
        db.add( newVol );
        break;
      }
    }
    newVol.setState( State.GENERATING );
    CreateStorageVolumeType scRequest = new CreateStorageVolumeType( newId, request.getSize(), request.getSnapshotId() );
    CreateStorageVolumeResponseType scReply = null;
    try {
      scReply = ( CreateStorageVolumeResponseType ) Messaging.send( StorageProperties.STORAGE_REF, scRequest );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Error calling CreateStorageVolume:" + e.getMessage() );
    }
    db.commit();
    CreateVolumeResponseType reply = ( CreateVolumeResponseType ) request.getReply();
    reply.setVolume( newVol.morph( new edu.ucsb.eucalyptus.msgs.Volume() ) );
    return reply;
  }

  public DeleteVolumeResponseType DeleteVolume( DeleteVolumeType request ) throws EucalyptusCloudException {
    DeleteVolumeResponseType reply = ( DeleteVolumeResponseType ) request.getReply();
    reply.set_return( false );
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    try {
      Volume vol = db.getUnique( Volume.named( userName, request.getVolumeId() ) );
      boolean isAttached = false;
      for( VmInstance vm : VmInstances.getInstance().listValues() ) {
        for( AttachedVolume attachedVol : vm.getVolumes() ) {
          if( request.getVolumeId().equals( attachedVol.getVolumeId() ) ) {
            isAttached = true;
          }
        }
      }
      if( isAttached ) return reply;
      if( !vol.getState(  ).equals( State.ANNILATED ) ) {
        Messaging.send( StorageProperties.STORAGE_REF, new DeleteStorageVolumeType( vol.getDisplayName() ) );
      }
      db.delete( vol );
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      return reply;
    }
    reply.set_return( true );
    return reply;
  }

  public DescribeVolumesResponseType DescribeVolumes( DescribeVolumesType request ) throws EucalyptusCloudException {
    DescribeVolumesResponseType reply = ( DescribeVolumesResponseType ) request.getReply();
    EntityWrapper<Volume> db = getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    LOG.debug( request );
    Messaging.send( StorageProperties.STORAGE_REF, new DescribeStorageVolumesType(  ) );

    Map<String, AttachedVolume> attachedVolumes = new HashMap<String, AttachedVolume>();
    for ( VmInstance vm : VmInstances.getInstance().listValues() ) {
      for ( AttachedVolume av : vm.getVolumes() ) {
        attachedVolumes.put( av.getVolumeId(), av );
      }
    }
    List<Volume> volumes = db.query( Volume.ownedBy( userName ) );
    for ( Volume v : volumes ) {
      if ( request.getVolumeSet().isEmpty() || request.getVolumeSet().contains( v.getDisplayName() ) ) {
        DescribeStorageVolumesResponseType volState = ( DescribeStorageVolumesResponseType ) Messaging.send( StorageProperties.STORAGE_REF, new DescribeStorageVolumesType( Lists.newArrayList( v.getDisplayName() ) ) );
        LOG.debug( volState );
        String volumeState = "unavailable";
        if ( !volState.getVolumeSet().isEmpty() ) {
          StorageVolume vol = volState.getVolumeSet().get( 0 );
          volumeState = vol.getStatus();
          v.setSize( new Integer( vol.getSize() ) );
          v.setRemoteDevice( vol.getActualDeviceName() );
        }
        if ( attachedVolumes.containsKey( v.getDisplayName() ) ) {
          volumeState = "in-use";
        }
        v.setMappedState( volumeState );
        edu.ucsb.eucalyptus.msgs.Volume aVolume = v.morph( new edu.ucsb.eucalyptus.msgs.Volume() );
        if ( attachedVolumes.containsKey( v.getDisplayName() ) ) {
          aVolume.setStatus( volumeState );
          aVolume.getAttachmentSet().add( attachedVolumes.get( aVolume.getVolumeId() ) );
        }
        reply.getVolumeSet().add( aVolume );
      }
    }
    db.commit();
    return reply;
  }

  public AttachVolumeResponseType AttachVolume( AttachVolumeType request ) throws EucalyptusCloudException {
    AttachVolumeResponseType reply = ( AttachVolumeResponseType ) request.getReply();

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId() );
    }
    for( AttachedVolume attachedVol : vm.getVolumes() ) {
      if( attachedVol.getDevice().replaceAll("unknown,requested:","").equals( request.getDevice() ) ) {
        throw new EucalyptusCloudException( "Already have a device attached to: " + request.getDevice() );
      }
    }
    Cluster cluster = null;
    try {
      cluster = Clusters.getInstance().lookup( vm.getPlacement() );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPlacement() );
    }
    for ( VmInstance v : VmInstances.getInstance().listValues() ) {
      for ( AttachedVolume vol : v.getVolumes() ) {
        if ( vol.getVolumeId().equals( request.getVolumeId() ) ) {
          throw new EucalyptusCloudException( "Volume already attached: " + request.getVolumeId() );
        }
      }
    }
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    Volume volume = null;
    try {
      volume = db.getUnique( Volume.named( userName, request.getVolumeId() ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId() );
    }

    request.setRemoteDevice( volume.getRemoteDevice() );
    QueuedEvent<AttachVolumeType> event = QueuedEvent.make( new VolumeAttachCallback( cluster.getConfiguration( ) ), request );
    cluster.getMessageQueue().enqueue( event );

    AttachedVolume attachVol = new AttachedVolume( volume.getDisplayName(), vm.getInstanceId(), request.getDevice(), volume.getRemoteDevice() );
    vm.getVolumes().add( attachVol );
    reply.setAttachedVolume( attachVol );

    return reply;
  }

  public DetachVolumeResponseType DetachVolume( DetachVolumeType request ) throws EucalyptusCloudException {
    DetachVolumeResponseType reply = ( DetachVolumeResponseType ) request.getReply();

    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    try {
      db.getUnique( Volume.named( userName, request.getVolumeId() ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId() );
    }
    db.commit();

    VmInstance vm = null;
    AttachedVolume volume = null;
    for ( VmInstance v : VmInstances.getInstance().listValues() ) {
      for ( AttachedVolume vol : v.getVolumes() ) {
        if ( vol.getVolumeId().equals( request.getVolumeId() ) ) {
          volume = vol;
          vm = v;
        }
      }
    }
    if ( volume == null || vm == null ) {
      throw new EucalyptusCloudException( "Volume is not attached: " + request.getVolumeId() );
    }
    if( !vm.getInstanceId().equals( request.getInstanceId() ) && request.getInstanceId() != null && !request.getInstanceId().equals("" ) ) {
      throw new EucalyptusCloudException( "Volume is not attached to instance: " + request.getInstanceId() );
    }
    if ( request.getDevice() != null && !request.getDevice().equals("") && !volume.getDevice().equals( request.getDevice() ) ) {
      throw new EucalyptusCloudException( "Volume is not attached to device: " + request.getDevice() );
    }

    Cluster cluster = null;
    try {
      cluster = Clusters.getInstance().lookup( vm.getPlacement() );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPlacement() );
    }

    request.setVolumeId( volume.getVolumeId() );
    request.setRemoteDevice( volume.getRemoteDevice() );
    request.setDevice( volume.getDevice().replaceAll("unknown,requested:","") );
    request.setInstanceId( vm.getInstanceId() );
    QueuedEvent<DetachVolumeType> event = QueuedEvent.make( new VolumeDetachCallback( cluster.getConfiguration( ) ), request );
    cluster.getMessageQueue().enqueue( event );

    reply.setDetachedVolume( volume );
    return reply;
  }

}

