/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.blockstorage;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.state.State;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;

public class SnapshotManager {
  
  private static Logger LOG       = Logger.getLogger( SnapshotManager.class );
  private static String ID_PREFIX = "snap";
  
  public static EntityWrapper<Snapshot> getEntityWrapper( ) {
    return new EntityWrapper<Snapshot>( VolumeManager.PERSISTENCE_CONTEXT );
  }
  
  public CreateSnapshotResponseType create( CreateSnapshotType request ) throws EucalyptusCloudException {
    
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    Volume vol = db.recast( Volume.class ).getUnique( Volume.named( userName, request.getVolumeId( ) ) );
    StorageControllerConfiguration sc;
    try {
      sc = Configuration.getStorageControllerConfiguration( vol.getCluster( ) );
    } catch ( Exception e ) {
      db.rollback( );
      throw new EucalyptusCloudException(
                                          "Failed to find the storage controller information for volume: " + vol.getDisplayName( ) + " at " + vol.getCluster( ),
                                          e );
    }
    
    if ( !vol.isReady( ) ) {
      //temporary workaround to update the volume state.
      DescribeStorageVolumesType descVols = new DescribeStorageVolumesType( Lists.newArrayList( vol.getDisplayName( ) ) );
      try {
        DescribeStorageVolumesResponseType volState = ServiceDispatcher.lookup( Component.storage, sc.getHostName( ) )
                                                                       .send( descVols, DescribeStorageVolumesResponseType.class );
        if ( !volState.getVolumeSet( ).isEmpty( ) ) {
          vol.setMappedState( volState.getVolumeSet( ).get( 0 ).getStatus( ) );
        } else {
          throw new EucalyptusCloudException( "Failed to update the volume state " + request.getVolumeId( ) + " not yet ready" );
        }
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
        db.rollback( );
        throw new EucalyptusCloudException( "Failed to update the volume state " + request.getVolumeId( ) + " not yet ready" );
      }
      if ( !vol.isReady( ) ) {
        db.rollback( );
        throw new EucalyptusCloudException( "Volume " + request.getVolumeId( ) + " not yet ready" );
      }
    }
    
    String newId = null;
    Snapshot snap = null;
    while ( true ) {
      newId = Crypto.generateId( request.getUserId( ), ID_PREFIX );
      try {
        db.getUnique( Snapshot.ownedBy( newId ) );
      } catch ( EucalyptusCloudException e ) {
        snap = new Snapshot( request.getUserId( ), newId, vol.getDisplayName( ) );
        db.add( snap );
        break;
      }
    }
    
    CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName( ), newId );
    CreateStorageSnapshotResponseType scReply = null;
    try {
      scReply = StorageUtil.lookup( sc.getHostName( ) ).send( scRequest, CreateStorageSnapshotResponseType.class );
      snap.setCluster( sc.getName( ) );
      snap.setMappedState( scReply.getStatus( ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Error calling CreateStorageSnapshot:" + e.getMessage( ) );
    }
    db.commit( );
    EventRecord.here( SnapshotManager.class, EventClass.SNAPSHOT, EventType.SNAPSHOT_CREATE, "user=" + snap.getUserName( ), "snapshot=" + snap.getDisplayName( ),
                      "volume=" + snap.getParentVolume( ) ).info( );
    
    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply( );
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
    snapMsg.setProgress( "0%" );
    snapMsg.setOwnerId( snap.getUserName( ) );
    snapMsg.setVolumeSize( vol.getSize( ).toString( ) );
    reply.setSnapshot( snapMsg );
    return reply;
  }
  
  public DeleteSnapshotResponseType delete( DeleteSnapshotType request ) throws EucalyptusCloudException {
    DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply( );
    reply.set_return( false );
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    try {
      Snapshot snap = db.getUnique( Snapshot.named( userName, request.getSnapshotId( ) ) );
      if ( !State.EXTANT.equals( snap.getState() ) ) {
        db.rollback();
    	reply.set_return( false );
    	return reply;
      }
      db.delete( snap );
      db.getSession( ).flush( );
      DeleteStorageSnapshotResponseType scReply = StorageUtil.send( snap.getCluster( ), new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
      if ( scReply.get_return( ) ) {
        StorageUtil.dispatchAll( new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
        db.commit( );
        EventRecord.here( SnapshotManager.class, EventClass.SNAPSHOT, EventType.SNAPSHOT_DELETE, "user=" + snap.getUserName( ), "snapshot=" + snap.getDisplayName( ) ).info( );
      } else {
    	db.rollback();
        throw new EucalyptusCloudException( "Unable to delete snapshot." );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Error deleting storage volume:" + e.getMessage( ) );
    }
    reply.set_return( true );
    return reply;
  }
  
  public DescribeSnapshotsResponseType describe( DescribeSnapshotsType request ) throws EucalyptusCloudException {
    DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply( );
    String userName = request.isAdministrator( ) ? null : request.getUserId( );
    
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper( );
    try {
      List<Snapshot> snapshots = db.query( Snapshot.ownedBy( userName ) );
      
      for ( Snapshot v : snapshots ) {
        DescribeStorageSnapshotsType scRequest = new DescribeStorageSnapshotsType( Lists.newArrayList( v.getDisplayName( ) ) );
        if ( request.getSnapshotSet( ).isEmpty( ) || request.getSnapshotSet( ).contains( v.getDisplayName( ) ) ) {
          try {
            StorageControllerConfiguration sc = Configuration.getStorageControllerConfiguration( v.getCluster( ) );
            DescribeStorageSnapshotsResponseType snapshotInfo = StorageUtil.lookup( sc.getHostName( ) ).send( scRequest,
                                                                                                              DescribeStorageSnapshotsResponseType.class );
            for ( StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet( ) ) {
              v.setMappedState( storageSnapshot.getStatus( ) );
              edu.ucsb.eucalyptus.msgs.Snapshot snapReply = v.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
              if ( storageSnapshot.getProgress( ) != null ) snapReply.setProgress( storageSnapshot.getProgress( ) );
              snapReply.setVolumeId( storageSnapshot.getVolumeId( ) );
              snapReply.setOwnerId( v.getUserName( ) );
              reply.getSnapshotSet( ).add( snapReply );
            }
          } catch ( EucalyptusCloudException e ) {
            LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
            LOG.debug( e, e );
          }
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return reply;
  }
}
