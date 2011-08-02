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
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.NoSuchComponentException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeClerk;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;

public class SnapshotManager {
  
  static Logger LOG       = Logger.getLogger( SnapshotManager.class );
  static String ID_PREFIX = "snap";
  
  public CreateSnapshotResponseType create( CreateSnapshotType request ) throws EucalyptusCloudException, NoSuchComponentException {
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
    Volume vol;
    try {
      vol = Transactions.find( Volume.named( ctx.getUserFullName( ), request.getVolumeId( ) ) );
    } catch ( ExecutionException ex1 ) {
      throw new EucalyptusCloudException( ex1 );
    }
    ServiceConfiguration sc = Partitions.lookupService( Storage.class, vol.getPartition( ) );
    vol = Volumes.checkVolumeReady( vol );
    Snapshot snap = Snapshots.initializeSnapshot( ctx.getUserFullName( ), vol, sc );
    snap = Snapshots.startCreateSnapshot( vol, snap );
    
    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply( );
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
    snapMsg.setProgress( "0%" );
    snapMsg.setOwnerId( snap.getOwnerAccountNumber( ) );
    snapMsg.setVolumeSize( vol.getSize( ).toString( ) );
    reply.setSnapshot( snapMsg );
    return reply;
  }
  
  /**
   * @deprecated Use {@link Snapshots#startCreateSnapshot(Volume,Snapshot)} instead
   */
  private static Snapshot startCreateSnapshot( final Volume vol, final Snapshot snap ) throws EucalyptusCloudException {
    return Snapshots.startCreateSnapshot( vol, snap );
  }
  
  public DeleteSnapshotResponseType delete( final DeleteSnapshotType request ) throws EucalyptusCloudException {
    final DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    boolean result = false;
    try {
      result = Transactions.delete( Snapshots.named( ctx.getUserFullName( ), request.getSnapshotId( ) ), new Predicate<Snapshot>( ) {
        
        @Override
        public boolean apply( Snapshot snap ) {
          if ( !State.EXTANT.equals( snap.getState( ) ) ) {
            return false;
          } else if ( !TypeClerk.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, request.getSnapshotId( ), snap.getOwner( ) ) ) {
            throw Exceptions.undeclared( "Not authorized to delete snapshot " + request.getSnapshotId( ) + " by " + ctx.getUser( ).getName( ),
                                         new EucalyptusCloudException( ) );
          } else {
            ServiceConfiguration sc = Partitions.lookupService( Storage.class, snap.getVolumePartition( ) );
            try {
              DeleteStorageSnapshotResponseType scReply = ServiceDispatcher.lookup( sc ).send( new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
              if ( scReply.get_return( ) ) {
                StorageUtil.dispatchAll( new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
                try {
                  ListenerRegistry.getInstance( ).fireEvent( new StorageEvent( StorageEvent.EventType.EbsSnapshot, true, snap.getVolumeSize( ),
                                                                               snap.getOwnerUserId( ),
                                                                               snap.getOwnerAccountNumber( ), snap.getVolumeCluster( ), snap.getVolumePartition( ) ) );
                } catch ( EventFailedException ex ) {
                  LOG.error( ex, ex );
                }
              } else {
                throw Exceptions.undeclared( "Unable to delete snapshot: " + snap, new EucalyptusCloudException( ) );
              }
            } catch ( EucalyptusCloudException ex1 ) {
              throw Exceptions.undeclared( ex1.getMessage( ), ex1 );
            }
            return true;
          }
        }
      } );
    } catch ( ExecutionException ex1 ) {
      throw new EucalyptusCloudException( ex1.getCause( ) );
    }
    reply.set_return( result );
    return reply;
  }
  
  public DescribeSnapshotsResponseType describe( DescribeSnapshotsType request ) throws EucalyptusCloudException {
    DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    try {
      List<Snapshot> snapshots = db.query( Snapshots.named( ctx.getUserFullName( ), null ) );
      
      for ( Snapshot snap : snapshots ) {
        if ( !TypeClerk.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT, snap.getDisplayName( ), snap.getOwner( ) ) ) {
          LOG.debug( "Skip snapshot " + snap.getDisplayName( ) + " due to access right" );
          continue;
        }
        DescribeStorageSnapshotsType scRequest = new DescribeStorageSnapshotsType( Lists.newArrayList( snap.getDisplayName( ) ) );
        if ( request.getSnapshotSet( ).isEmpty( ) || request.getSnapshotSet( ).contains( snap.getDisplayName( ) ) ) {
          try {
            ServiceConfiguration sc = Partitions.lookupService( Storage.class, snap.getPartition( ) );
            DescribeStorageSnapshotsResponseType snapshotInfo = ServiceDispatcher.lookup( sc ).send( scRequest );
            for ( StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet( ) ) {
              snap.setMappedState( storageSnapshot.getStatus( ) );
              edu.ucsb.eucalyptus.msgs.Snapshot snapReply = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
              if ( storageSnapshot.getProgress( ) != null ) snapReply.setProgress( storageSnapshot.getProgress( ) );
              snapReply.setVolumeId( storageSnapshot.getVolumeId( ) );
              snapReply.setOwnerId( snap.getOwnerAccountNumber( ) );
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
