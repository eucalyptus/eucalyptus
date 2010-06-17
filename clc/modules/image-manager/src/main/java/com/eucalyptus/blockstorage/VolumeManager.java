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
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.VolumeAttachCallback;
import com.eucalyptus.cluster.callback.VolumeDetachCallback;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.cloud.state.State;
import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeType;

public class VolumeManager {
  static String         PERSISTENCE_CONTEXT = "eucalyptus_images";
  
  private static String ID_PREFIX           = "vol";
  private static Logger LOG                 = Logger.getLogger( VolumeManager.class );
  
  public static EntityWrapper<Volume> getEntityWrapper( ) {
    return new EntityWrapper<Volume>( PERSISTENCE_CONTEXT );
  }
  
  public CreateVolumeResponseType CreateVolume( CreateVolumeType request ) throws EucalyptusCloudException {
    if ( ( request.getSnapshotId( ) == null && request.getSize( ) == null ) ) {
      throw new EucalyptusCloudException( "One of size or snapshotId is required as a parameter." );
    }
    try {
      Configuration.getClusterConfiguration( request.getAvailabilityZone( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Zone does not exist: " + request.getAvailabilityZone( ), e );
    }
    StorageControllerConfiguration sc;
    try {
      sc = Configuration.getStorageControllerConfiguration( request.getAvailabilityZone( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Storage services are not available for the requested availability zone.", e );
    }
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper( );
    if ( request.getSnapshotId( ) != null ) {
      String userName = request.isAdministrator( ) ? null : request.getUserId( );
      try {
        db.recast( Snapshot.class ).getUnique( Snapshot.named( userName, request.getSnapshotId( ) ) );
      } catch ( EucalyptusCloudException e ) {
        LOG.debug( e, e );
        db.rollback( );
        throw new EucalyptusCloudException( "Snapshot does not exist: " + request.getSnapshotId( ) );
      }
    }
    String newId = null;
    Volume newVol = null;
    while ( true ) {
      newId = Crypto.generateId( request.getUserId( ), ID_PREFIX );
      try {
        db.getUnique( Volume.named( null, newId ) );
      } catch ( EucalyptusCloudException e ) {
        try {
          newVol = new Volume( request.getUserId( ), newId, new Integer( request.getSize( ) != null ? request.getSize( ) : "-1" ),
                               request.getAvailabilityZone( ), request.getSnapshotId( ) );
          db.add( newVol );
          db.commit( );
          break;
        } catch ( Throwable e1 ) {
          db.rollback( );
          db = VolumeManager.getEntityWrapper( );
        }
      }
    }
    newVol.setState( State.GENERATING );
    try {
      CreateStorageVolumeType req = new CreateStorageVolumeType( newId, request.getSize( ), request.getSnapshotId( ) );
      req.setUserId( request.getUserId( ) );
      req.setEffectiveUserId( request.getEffectiveUserId( ) );
      StorageUtil.send( sc.getName( ), req );
      EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_CREATE, "user=" + newVol.getUserName( ), "volume=" + newVol.getDisplayName( ),
                        "size=" + newVol.getSize( ), "cluster=" + newVol.getCluster( ), "snapshot=" + newVol.getParentSnapshot( ) ).info( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      try {
        db = VolumeManager.getEntityWrapper( );
        Volume d = db.getUnique( Volume.named( newVol.getUserName( ), newVol.getDisplayName( ) ) );
        db.delete( d );
        db.commit( );
      } catch ( Throwable e1 ) {
        db.rollback( );
        LOG.debug( e1, e1 );
      }
      throw new EucalyptusCloudException( "Error while communicating with Storage Controller: CreateStorageVolume:" + e.getMessage( ) );
    }
    CreateVolumeResponseType reply = ( CreateVolumeResponseType ) request.getReply( );
    reply.setVolume( newVol.morph( new edu.ucsb.eucalyptus.msgs.Volume( ) ) );
    return reply;
  }
  
  public DeleteVolumeResponseType DeleteVolume( DeleteVolumeType request ) throws EucalyptusCloudException {
    DeleteVolumeResponseType reply = ( DeleteVolumeResponseType ) request.getReply( );
    reply.set_return( false );
    
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    boolean reallyFailed = false;
    try {
      Volume vol = db.getUnique( Volume.named( userName, request.getVolumeId( ) ) );
      for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        for ( AttachedVolume attachedVol : vm.getVolumes( ) ) {
          if ( request.getVolumeId( ).equals( attachedVol.getVolumeId( ) ) ) {
            db.rollback( );
            return reply;
          }
        }
      }
      if ( State.FAIL.equals( vol.getState( ) ) ) {
        db.delete( vol );
        db.commit( );
        return reply;
      }
      DeleteStorageVolumeResponseType scReply = StorageUtil.send( vol.getCluster( ), new DeleteStorageVolumeType( vol.getDisplayName( ) ) );
      if ( scReply.get_return( ) ) {
        vol.setState( State.ANNIHILATING );
        db.commit( );
        EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_DELETE, "user=" + vol.getUserName( ), "volume=" + vol.getDisplayName( ),
                          "size=" + vol.getSize( ), "cluster=" + vol.getCluster( ), "snapshot=" + vol.getParentSnapshot( ) ).info( );
      } else {
        reallyFailed = true;
        throw new EucalyptusCloudException( "Storage Controller returned false:  Contact the administrator to report the problem." );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      if ( reallyFailed ) {
        throw e;
      } else {
        return reply;
      }
    }
    reply.set_return( true );
    return reply;
  }
  
  public DescribeVolumesResponseType DescribeVolumes( DescribeVolumesType request ) throws EucalyptusCloudException {
    DescribeVolumesResponseType reply = ( DescribeVolumesResponseType ) request.getReply( );
    EntityWrapper<Volume> db = getEntityWrapper( );
    try {
      String userName = request.isAdministrator( ) ? null : request.getUserId( );
      
      Map<String, AttachedVolume> attachedVolumes = new HashMap<String, AttachedVolume>( );
      for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        for ( AttachedVolume av : vm.getVolumes( ) ) {
          attachedVolumes.put( av.getVolumeId( ), av );
        }
      }
      
      List<Volume> volumes = db.query( Volume.ownedBy( userName ) );
      List<Volume> describeVolumes = Lists.newArrayList( );
      for ( Volume v : volumes ) {
        if ( !State.ANNIHILATED.equals( v.getState( ) ) ) {
          describeVolumes.add( v );
        } else {
          db.delete( v );
        }
      }
      try {
        ArrayList<edu.ucsb.eucalyptus.msgs.Volume> volumeReplyList = StorageUtil.getVolumeReply( attachedVolumes, describeVolumes );
        reply.getVolumeSet( ).addAll( volumeReplyList );
      } catch ( Exception e ) {
        LOG.warn( "Error getting volume information from the Storage Controller: " + e );
        LOG.debug( e, e );
      }
      db.commit( );
    } catch ( Throwable t ) {
      db.commit( );
    }
    return reply;
  }
  
  public AttachVolumeResponseType AttachVolume( AttachVolumeType request ) throws EucalyptusCloudException {
    AttachVolumeResponseType reply = ( AttachVolumeResponseType ) request.getReply( );
    
    if ( request.getDevice( ) == null || request.getDevice( ).endsWith( "sda" ) ) {
      throw new EucalyptusCloudException( "Invalid device name specified: " + request.getDevice( ) );
    }
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId( ) );
    }
    for ( AttachedVolume attachedVol : vm.getVolumes( ) ) {
      if ( attachedVol.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( request.getDevice( ) ) ) {
        throw new EucalyptusCloudException( "Already have a device attached to: " + request.getDevice( ) );
      }
    }
    Cluster cluster = null;
    try {
      cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPlacement( ) );
    }
    
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      for ( AttachedVolume vol : v.getVolumes( ) ) {
        if ( vol.getVolumeId( ).equals( request.getVolumeId( ) ) ) {
          throw new EucalyptusCloudException( "Volume already attached: " + request.getVolumeId( ) );
        }
      }
    }
    
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    Volume volume = null;
    try {
      volume = db.getUnique( Volume.named( userName, request.getVolumeId( ) ) );
      if ( volume.getRemoteDevice( ) == null ) {
        StorageUtil.getVolumeReply( new HashMap<String,AttachedVolume>(), Lists.newArrayList( volume ) );
      }
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId( ) );
    }
    
    if ( !volume.getCluster( ).equals( cluster.getName( ) ) ) {
      throw new EucalyptusCloudException( "Can only attach volumes in the same cluster: " + request.getVolumeId( ) );
    } else if ( "invalid".equals( volume.getRemoteDevice( ) ) ) {
      throw new EucalyptusCloudException( "Volume is not yet available: " + request.getVolumeId( ) );
    }
    request.setRemoteDevice( volume.getRemoteDevice( ) );
    new VolumeAttachCallback( request ).dispatch( cluster );
    
    AttachedVolume attachVol = new AttachedVolume( volume.getDisplayName( ), vm.getInstanceId( ), request.getDevice( ), volume.getRemoteDevice( ) );
    attachVol.setStatus( "attaching" );
    vm.getVolumes( ).add( attachVol );
    EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_ATTACH, "user=" + volume.getUserName( ), "volume=" + volume.getDisplayName( ),
                      "instance=" + vm.getInstanceId( ), "cluster=" + vm.getPlacement( ) ).info( );
    volume.setState( State.BUSY );
    reply.setAttachedVolume( attachVol );
    return reply;
  }
  
  public DetachVolumeResponseType detach( DetachVolumeType request ) throws EucalyptusCloudException {
    DetachVolumeResponseType reply = ( DetachVolumeResponseType ) request.getReply( );
    
    EntityWrapper<Volume> db = VolumeManager.getEntityWrapper( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    try {
      db.getUnique( Volume.named( userName, request.getVolumeId( ) ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId( ) );
    }
    db.commit( );
    
    VmInstance vm = null;
    AttachedVolume volume = null;
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      for ( AttachedVolume vol : v.getVolumes( ) ) {
        if ( vol.getVolumeId( ).equals( request.getVolumeId( ) ) ) {
          volume = vol;
          vm = v;
        }
      }
    }
    if ( volume == null ) {
      throw new EucalyptusCloudException( "Volume is not attached: " + request.getVolumeId( ) );
    }
    if ( !vm.getInstanceId( ).equals( request.getInstanceId( ) ) && request.getInstanceId( ) != null && !request.getInstanceId( ).equals( "" ) ) {
      throw new EucalyptusCloudException( "Volume is not attached to instance: " + request.getInstanceId( ) );
    }
    if ( request.getDevice( ) != null && !request.getDevice( ).equals( "" ) && !volume.getDevice( ).equals( request.getDevice( ) ) ) {
      throw new EucalyptusCloudException( "Volume is not attached to device: " + request.getDevice( ) );
    }
    
    Cluster cluster = null;
    try {
      cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPlacement( ) );
    }
    
    request.setVolumeId( volume.getVolumeId( ) );
    request.setRemoteDevice( volume.getRemoteDevice( ) );
    request.setDevice( volume.getDevice( ).replaceAll( "unknown,requested:", "" ) );
    request.setInstanceId( vm.getInstanceId( ) );
    new VolumeDetachCallback( request ).dispatch( cluster );
    EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_DETACH, "user=" + vm.getOwnerId( ), "volume=" + volume.getVolumeId( ),
                      "instance=" + vm.getInstanceId( ), "cluster=" + vm.getPlacement( ) ).info( );
    volume.setStatus( "detaching" );
    reply.setDetachedVolume( volume );
    return reply;
  }
  
}
