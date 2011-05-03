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
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.blockstorage;

import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.NoSuchComponentException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;
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
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;

public class SnapshotManager {
  
  private static Logger LOG       = Logger.getLogger( SnapshotManager.class );
  private static String ID_PREFIX = "snap";
  
  public CreateSnapshotResponseType create( CreateSnapshotType request ) throws EucalyptusCloudException {    
    Context ctx = Contexts.lookup( );
    String action = PolicySpec.requestToAction( request );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, "", ctx.getAccount( ), action, ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to create snapshot by " + ctx.getUser( ).getName( ) );
      }
      if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, "", action, ctx.getUser( ), 1L ) ) {
        throw new EucalyptusCloudException( "Quota exceeded in creating snapshot by " + ctx.getUser( ).getName( ) );
      }
    }
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    Volume vol = db.recast( Volume.class ).getUnique( Volume.named( ctx.getUserFullName( ), request.getVolumeId( ) ) );
    ServiceConfiguration sc = null;
    try {
      sc = Partitions.lookupService( Storage.class, vol.getPartition( ) );
    } catch ( NoSuchComponentException ex ) {
      throw new EucalyptusCloudException( ex );
    }
    
    if ( !vol.isReady( ) ) {
      //temporary workaround to update the volume state.
      DescribeStorageVolumesType descVols = new DescribeStorageVolumesType( Lists.newArrayList( vol.getDisplayName( ) ) );
      try {
        DescribeStorageVolumesResponseType volState = ServiceDispatcher.lookup( sc )
                                                                       .send( descVols );
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
      newId = Crypto.generateId( ctx.getUserFullName( ).getUniqueId( ), ID_PREFIX );
      try {
        db.getUnique( Snapshot.named( newId ) );
      } catch ( EucalyptusCloudException e ) {
        snap = new Snapshot( ctx.getUserFullName( ), newId, vol.getDisplayName( ), sc.getName( ), sc.getPartition( ) );
        snap.setVolumeSize( vol.getSize( ) );
        db.add( snap );
        break;
      }
    }
    
    CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName( ), newId );
    CreateStorageSnapshotResponseType scReply = null;
    try {
      scReply = ServiceDispatcher.lookup( sc ).send( scRequest );
      snap.setMappedState( scReply.getStatus( ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Error calling CreateStorageSnapshot:" + e.getMessage( ), e );
    }
    db.commit( );
    try {
      ListenerRegistry.getInstance( ).fireEvent( new StorageEvent( StorageEvent.EventType.EbsSnapshot, true, snap.getVolumeSize( ), snap.getOwnerUserId( ), snap.getOwnerAccountId( ), snap.getVolumeCluster( ), snap.getVolumePartition( ) ) );
    } catch ( EventFailedException ex ) {
      LOG.error( ex , ex );
    }

    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply( );
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
    snapMsg.setProgress( "0%" );
    snapMsg.setOwnerId( snap.getOwnerAccountId( ) );
    snapMsg.setVolumeSize( vol.getSize( ).toString( ) );
    reply.setSnapshot( snapMsg );
    return reply;
  }
  
  public DeleteSnapshotResponseType delete( DeleteSnapshotType request ) throws EucalyptusCloudException {
    DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply( );
    reply.set_return( false );
    Context ctx = Contexts.lookup( );
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    try {
      Snapshot snap = db.getUnique( Snapshot.named( ctx.getUserFullName( ) , request.getSnapshotId( ) ) );
      if ( !State.EXTANT.equals( snap.getState( ) ) ) {
        db.rollback( );
        reply.set_return( false );
        return reply;
      }
      if ( !Lookups.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, request.getSnapshotId( ), snap.getOwner( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to delete snapshot " + request.getSnapshotId( ) + " by " + ctx.getUser( ).getName( ) );
      }
      db.delete( snap );
//      db.getSession( ).flush( );
      ServiceConfiguration sc = null;
      try {
        sc = Partitions.lookupService( Storage.class, snap.getVolumePartition( ) );
      } catch ( NoSuchElementException e ) {
      } catch ( NoSuchComponentException ex ) {
        throw new EucalyptusCloudException( "Failed to find the storage controller information for volume: "
                                            + snap.getDisplayName( ) + " at " + snap.getVolumePartition( ) + " because of " + ex.getMessage( ), ex );
      }

      DeleteStorageSnapshotResponseType scReply = ServiceDispatcher.lookup( sc ).send( new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
      if ( scReply.get_return( ) ) {
        StorageUtil.dispatchAll( new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
        db.commit( );
        try {
          ListenerRegistry.getInstance( ).fireEvent( new StorageEvent( StorageEvent.EventType.EbsSnapshot, true, snap.getVolumeSize( ), snap.getOwnerUserId( ), snap.getOwnerAccountId( ), snap.getVolumeCluster( ), snap.getVolumePartition( ) ) );
        } catch ( EventFailedException ex ) {
          LOG.error( ex , ex );
        }
      } else {
        db.rollback( );
        throw new EucalyptusCloudException( "Unable to delete snapshot." );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new EucalyptusCloudException( "Error deleting storage volume:" + e.getMessage( ), e );
    }
    reply.set_return( true );
    return reply;
  }
  
  public DescribeSnapshotsResponseType describe( DescribeSnapshotsType request ) throws EucalyptusCloudException {
    DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    try {
      List<Snapshot> snapshots = db.query( Snapshot.ownedBy( ctx.getUserFullName( ) ) );
      
      for ( Snapshot v : snapshots ) {
        if ( !Lookups.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, v.getDisplayName( ), v.getOwner( ) ) ) {
          LOG.debug( "Skip snapshot " + v.getDisplayName( ) + " due to access right" );
          continue;
        }
        DescribeStorageSnapshotsType scRequest = new DescribeStorageSnapshotsType( Lists.newArrayList( v.getDisplayName( ) ) );
        if ( request.getSnapshotSet( ).isEmpty( ) || request.getSnapshotSet( ).contains( v.getDisplayName( ) ) ) {
          try {
            ServiceConfiguration sc = Partitions.lookupService( Storage.class, v.getPartition( ) );
            DescribeStorageSnapshotsResponseType snapshotInfo = ServiceDispatcher.lookup( sc ).send( scRequest );
            for ( StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet( ) ) {
              v.setMappedState( storageSnapshot.getStatus( ) );
              edu.ucsb.eucalyptus.msgs.Snapshot snapReply = v.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
              if ( storageSnapshot.getProgress( ) != null ) snapReply.setProgress( storageSnapshot.getProgress( ) );
              snapReply.setVolumeId( storageSnapshot.getVolumeId( ) );
              snapReply.setOwnerId( v.getOwnerAccountId( ) );
              reply.getSnapshotSet( ).add( snapReply );
            }
          } catch ( NoSuchElementException e ) {
            LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
            LOG.debug( e, e );
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
  
  public ResetSnapshotAttributeResponseType resetSnapshotAttribute( ResetSnapshotAttributeType request ) {
    ResetSnapshotAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public ModifySnapshotAttributeResponseType modifySnapshotAttribute( ModifySnapshotAttributeType request ) {
    ModifySnapshotAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribeSnapshotAttributeResponseType describeSnapshotAttribute( DescribeSnapshotAttributeType request ) {
    DescribeSnapshotAttributeResponseType reply = request.getReply( );
    return reply;
  }
}
