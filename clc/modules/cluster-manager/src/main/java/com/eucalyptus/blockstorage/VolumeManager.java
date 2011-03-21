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
 *******************************************************************************
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.VolumeAttachCallback;
import com.eucalyptus.cluster.callback.VolumeDetachCallback;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.util.async.Callbacks;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVolumeType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVolumesType;
import edu.ucsb.eucalyptus.msgs.DetachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeType;

public class VolumeManager {
  private static String ID_PREFIX           = "vol";
  private static Logger LOG                 = Logger.getLogger( VolumeManager.class );
  
  public CreateVolumeResponseType CreateVolume( final CreateVolumeType request ) throws EucalyptusCloudException {
    Context ctx = Contexts.lookup( );
    String action = PolicySpec.requestToAction( request );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      if ( !Permissions.isAuthorized( PolicySpec.EC2_RESOURCE_VOLUME, "", ctx.getAccount( ), action, ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to create volume by " + ctx.getUser( ).getName( ) );
      }
      long volSize = 0;
      try {
        volSize = Long.valueOf( request.getSize( ) );
      } catch ( NumberFormatException e ) {
        LOG.error( "Invalid volume size " + request.getSize( ), e );
        throw new EucalyptusCloudException( e );
      }
      if ( !Permissions.canAllocate( PolicySpec.EC2_RESOURCE_VOLUME, "", action, ctx.getUser( ), volSize ) ) {
        throw new EucalyptusCloudException( "Exceeded quota of volume creation by " + ctx.getUser( ).getName( ) );
      }
    }
    if ( ( request.getSnapshotId( ) == null && request.getSize( ) == null ) ) {
      throw new EucalyptusCloudException( "One of size or snapshotId is required as a parameter." );
    }
    ServiceConfiguration sc;
    try {
      sc = StorageUtil.getActiveSc( request.getAvailabilityZone( ) ).getServiceConfiguration( );
    } catch ( NoSuchElementException ex ) {
      throw new EucalyptusCloudException( ex.getMessage( ), ex );
    }
    EntityWrapper<Volume> db = EntityWrapper.get( Volume.class );
    if ( request.getSnapshotId( ) != null ) {
      try {
        db.recast( Snapshot.class ).getUnique( Snapshot.named( ctx.getUserFullName( ), request.getSnapshotId( ) ) );
      } catch ( EucalyptusCloudException e ) {
        LOG.debug( e, e );
        db.rollback( );
        throw new EucalyptusCloudException( "Snapshot does not exist: " + request.getSnapshotId( ) );
      }
    }
    String newId = null;
    Volume newVol = null;
    while ( true ) {
      newId = Crypto.generateId( ctx.getUserFullName( ).getAccountId( ), ID_PREFIX );
      try {
        db.getUnique( Volume.named( null, newId ) );
      } catch ( EucalyptusCloudException e ) {
        try {
          newVol = new Volume( ctx.getUserFullName( ), newId, new Integer( request.getSize( ) != null ? request.getSize( ) : "-1" ),
                               request.getAvailabilityZone( ), request.getSnapshotId( ) );
          db.add( newVol );
          db.commit( );
          break;
        } catch ( Throwable e1 ) {
          db.rollback( );
          db = EntityWrapper.get( Volume.class );
        }
      }
    }
    newVol.setState( State.GENERATING );
    try {
      CreateStorageVolumeType req = new CreateStorageVolumeType( newId, request.getSize( ), request.getSnapshotId( ) );
      req.regardingUserRequest( request );
      StorageUtil.send( sc.getName( ), req );
      EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_CREATE )
                 .withDetails( newVol.getOwner( ).toString( ), newVol.getDisplayName( ), "size", newVol.getSize( ).toString( ) )
                 .withDetails( "cluster", newVol.getCluster( ) ).withDetails( "snapshot", newVol.getParentSnapshot( ) ).info( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      try {
        db = EntityWrapper.get( Volume.class );
        Volume d = db.getUnique( Volume.named( (UserFullName) newVol.getOwner( ), newVol.getDisplayName( ) ) );
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
    Context ctx = Contexts.lookup( );
    reply.set_return( false );
    
    EntityWrapper<Volume> db = EntityWrapper.get( Volume.class );
    boolean reallyFailed = false;
    try {
      Volume vol = db.getUnique( Volume.named( ctx.getUserFullName( ), request.getVolumeId( ) ) );
      if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_VOLUME, request.getVolumeId( ), vol.getOwner( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to delete volume by " + ctx.getUser( ).getName( ) );
      }
      for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        try {
          vm.lookupVolumeAttachment( request.getVolumeId( ) );
          db.rollback( );
          return reply;
        } catch ( NoSuchElementException ex ) {
          /** no such volume attached, move along... **/
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
        EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_DELETE ).withDetails( vol.getOwner( ).toString( ), vol.getDisplayName( ) , "size", vol.getSize( ).toString( ) )
                          .withDetails( "cluster", vol.getCluster( ) ).withDetails( "snapshot", vol.getParentSnapshot( ) ).info( );
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
    Context ctx = Contexts.lookup( );
    EntityWrapper<Volume> db = EntityWrapper.get( Volume.class );
    try {
      
      final Map<String, AttachedVolume> attachedVolumes = new HashMap<String, AttachedVolume>( );
      for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
        vm.eachVolumeAttachment( new Predicate<AttachedVolume>() {
          @Override
          public boolean apply( AttachedVolume arg0 ) {
            return attachedVolumes.put( arg0.getVolumeId( ), arg0 ) == null;
          }} );
      }
      
      List<Volume> volumes = db.query( Volume.ownedBy( ctx.getUserFullName( ) ) );
      List<Volume> describeVolumes = Lists.newArrayList( );
      for ( Volume v : volumes ) {
        if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_VOLUME, v.getDisplayName( ), v.getOwner( ) ) ) {
          continue;
        }
        if ( !State.ANNIHILATED.equals( v.getState( ) ) ) {
          describeVolumes.add( v );
        } else {
          EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_DELETE )
                     .withDetails( v.getOwner( ).toString( ), v.getDisplayName( ), "size", v.getSize( ).toString( ) ).withDetails( "cluster", v.getCluster( ) )
                     .withDetails( "snapshot", v.getParentSnapshot( ) ).info( );
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
    Context ctx = Contexts.lookup( );
    
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
    if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_INSTANCE, request.getInstanceId( ), vm.getOwner( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to attach volume to instance " + request.getInstanceId( ) + " by " + ctx.getUser( ).getName( ) );
    }
    Cluster cluster = null;
    try {
      cluster = Clusters.getInstance( ).lookup( vm.getPlacement( ) );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPlacement( ) );
    }
    final String deviceName = request.getDevice( );
    final String volumeId = request.getVolumeId( );
    try {
      vm.lookupVolumeAttachment( new Predicate<AttachedVolume>( ) {
        @Override
        public boolean apply( AttachedVolume arg0 ) {
          return arg0.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( deviceName );
        }} );
      throw new EucalyptusCloudException( "Already have a device attached to: " + request.getDevice( ) );
    } catch ( NoSuchElementException ex1 ) {
      /** no attachment **/
    }
    for ( VmInstance iter : VmInstances.getInstance( ).listValues( ) ) {
      try {
        iter.lookupVolumeAttachment( volumeId );
        throw new EucalyptusCloudException( "Volume already attached: " + request.getVolumeId( ) );
      } catch ( NoSuchElementException ex ) {
        /** no attachment **/
      }
    }
    
    EntityWrapper<Volume> db = EntityWrapper.get( Volume.class );
    Volume volume = null;
    try {
      volume = db.getUnique( Volume.named( ctx.getUserFullName( ), request.getVolumeId( ) ) );
      if ( volume.getRemoteDevice( ) == null ) {
        StorageUtil.getVolumeReply( new HashMap<String,AttachedVolume>(), Lists.newArrayList( volume ) );
      }
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId( ) );
    }
    if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_VOLUME, request.getVolumeId( ), volume.getOwner( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to attach volume " + request.getVolumeId( ) + " by " + ctx.getUser( ).getName( ) );
    }
    ServiceConfiguration sc;
    try {
      sc = StorageUtil.getActiveSc( volume.getCluster( ) ).getServiceConfiguration( );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex , ex );
      throw new EucalyptusCloudException( "Failed to lookup SC for volume: " + volume, ex );
    }
    ServiceConfiguration scVm;
    try {
      scVm = StorageUtil.getActiveSc( cluster.getName( ) ).getServiceConfiguration( );
    } catch ( Exception ex ) {
      LOG.error( ex , ex );
      throw new EucalyptusCloudException( "Failed to lookup SC for cluster: " + cluster, ex );
    }
    if ( !sc.equals( scVm ) ) {
      throw new EucalyptusCloudException( "Can only attach volumes in the same cluster: " + request.getVolumeId( ) );
    } else if ( "invalid".equals( volume.getRemoteDevice( ) ) ) {
      throw new EucalyptusCloudException( "Volume is not yet available: " + request.getVolumeId( ) );
    }

    AttachStorageVolumeResponseType scAttachResponse;
    try {
      scAttachResponse = StorageUtil.send( sc.getName( ), new AttachStorageVolumeType( cluster.getNode( vm.getServiceTag( ) ).getIqn( ), volume.getDisplayName( ) ) );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    request.setRemoteDevice( scAttachResponse.getRemoteDeviceString( ) );
    AttachedVolume attachVol = new AttachedVolume( volume.getDisplayName( ), vm.getInstanceId( ), request.getDevice( ), request.getRemoteDevice( ) );
    attachVol.setStatus( "attaching" );
    Callbacks.newRequest( new VolumeAttachCallback( request, attachVol ) ).dispatch( cluster.getServiceEndpoint( ) );
    
    vm.addVolumeAttachment( attachVol );
    EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_ATTACH )
               .withDetails( volume.getOwner( ).toString( ), volume.getDisplayName( ), "instance", vm.getInstanceId( ) )
               .withDetails( "cluster", vm.getPlacement( ) ).info( );
    volume.setState( State.BUSY );
    reply.setAttachedVolume( attachVol );
    return reply;
  }
  
  public DetachVolumeResponseType detach( DetachVolumeType request ) throws EucalyptusCloudException {
    DetachVolumeResponseType reply = ( DetachVolumeResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );

    Volume vol = null;
    EntityWrapper<Volume> db = EntityWrapper.get( Volume.class );
    try {
      vol = db.getUnique( Volume.named( ctx.getUserFullName( ), request.getVolumeId( ) ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Volume does not exist: " + request.getVolumeId( ) );
    }
    db.commit( );
    if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_VOLUME, request.getVolumeId( ), vol.getOwner( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to detach volume " + request.getVolumeId( ) + " by " + ctx.getUser( ).getName( ) );
    }
    
    VmInstance vm = null;
    AttachedVolume volume = null;
    for ( VmInstance iter : VmInstances.getInstance( ).listValues( ) ) {
      try {
        volume = iter.lookupVolumeAttachment( request.getVolumeId( ) );
        vm = iter;
      } catch ( NoSuchElementException ex ) {
        /** no such attachment **/
      }
    }
    if ( volume == null ) {
      throw new EucalyptusCloudException( "Volume is not attached: " + request.getVolumeId( ) );
    }
    if ( !Lookups.checkPrivilege( request, PolicySpec.EC2_RESOURCE_INSTANCE, request.getInstanceId( ), vm.getOwner( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to detach volume from instance " + request.getInstanceId( ) + " by " + ctx.getUser( ).getName( ) );
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
    ServiceConfiguration scVm;
    try {
      scVm = StorageUtil.getActiveSc( cluster.getName( ) ).getServiceConfiguration( );
    } catch ( Exception ex ) {
      LOG.error( ex , ex );
      throw new EucalyptusCloudException( "Failed to lookup SC for cluster: " + cluster, ex );
    }
    try {
      StorageUtil.send( scVm.getName( ), new DetachStorageVolumeType( cluster.getNode( vm.getServiceTag( ) ).getIqn( ), volume.getVolumeId( ) ) );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    request.setVolumeId( volume.getVolumeId( ) );
    request.setRemoteDevice( volume.getRemoteDevice( ) );
    request.setDevice( volume.getDevice( ).replaceAll( "unknown,requested:", "" ) );
    request.setInstanceId( vm.getInstanceId( ) );
    Callbacks.newRequest( new VolumeDetachCallback( request ) ).dispatch( cluster.getServiceEndpoint( ) );
    EventRecord.here( VolumeManager.class, EventClass.VOLUME, EventType.VOLUME_DETACH )
               .withDetails( vm.getOwner( ).toString( ), volume.getVolumeId( ), "instance", vm.getInstanceId( ) ).withDetails( "cluster", vm.getPlacement( ) ).info( );
    volume.setStatus( "detaching" );
    reply.setDetachedVolume( volume );
    return reply;
  }
  
}
