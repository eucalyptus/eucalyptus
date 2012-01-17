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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.component.NoSuchComponentException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
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
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeType;

public class SnapshotManager {
  
  static Logger LOG       = Logger.getLogger( SnapshotManager.class );
  static String ID_PREFIX = "snap";
  
  public CreateSnapshotResponseType create( CreateSnapshotType request ) throws EucalyptusCloudException, NoSuchComponentException, DuplicateMetadataException, AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException, TransactionException {
    final Context ctx = Contexts.lookup( );
    EntityWrapper<Snapshot> db = EntityWrapper.get( Snapshot.class );
    Volume vol = Transactions.find( Volume.named( ctx.getUserFullName( ).asAccountFullName( ), request.getVolumeId( ) ) );
    final ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
    final Volume volReady = Volumes.checkVolumeReady( vol );
    Supplier<Snapshot> allocator = new Supplier<Snapshot>( ) {
      
      @Override
      public Snapshot get( ) {
        try {
          return Snapshots.initializeSnapshot( ctx.getUserFullName( ), volReady, sc );
        } catch ( EucalyptusCloudException ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    Snapshot snap = RestrictedTypes.allocateUnitlessResource( allocator );
    snap = Snapshots.startCreateSnapshot( volReady, snap );
    
    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply( );
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
    snapMsg.setProgress( "0%" );
    snapMsg.setOwnerId( snap.getOwnerAccountNumber( ) );
    snapMsg.setVolumeSize( volReady.getSize( ).toString( ) );
    reply.setSnapshot( snapMsg );
    return reply;
  }
  
  /**
   * @throws DuplicateMetadataException
   * @deprecated Use {@link Snapshots#startCreateSnapshot(Volume,Snapshot)} instead
   */
  private static Snapshot startCreateSnapshot( final Volume vol, final Snapshot snap ) throws EucalyptusCloudException, DuplicateMetadataException {
    return Snapshots.startCreateSnapshot( vol, snap );
  }
  
  public DeleteSnapshotResponseType delete( final DeleteSnapshotType request ) throws EucalyptusCloudException {
    final DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    boolean result = false;
    try {
      result = Transactions.delete( Snapshot.named( ctx.getUserFullName( ).asAccountFullName( ), request.getSnapshotId( ) ), new Predicate<Snapshot>( ) {
        
        @Override
        public boolean apply( Snapshot snap ) {
          if ( !State.EXTANT.equals( snap.getState( ) ) && !State.FAIL.equals( snap.getState( ) ) ) {
            return false;
          } else if ( !RestrictedTypes.filterPrivileged( ).apply( snap ) ) {
            throw Exceptions.toUndeclared( "Not authorized to delete snapshot " + request.getSnapshotId( ) + " by " + ctx.getUser( ).getName( ),
                                           new EucalyptusCloudException( ) );
          } else {
            Snapshots.fireDeleteEvent( snap );
            final ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( snap.getPartition( ) ) );
            try {
              DeleteStorageSnapshotResponseType scReply = AsyncRequests.sendSync( sc, new DeleteStorageSnapshotType( snap.getDisplayName( ) ) );
              if ( scReply.get_return( ) ) {
                final String snapshotId = snap.getDisplayName( );
                Callable<Boolean> deleteBroadcast = new Callable<Boolean>( ) {
                  public Boolean call( ) {
                    final DeleteStorageSnapshotType deleteMsg = new DeleteStorageSnapshotType( snapshotId );
                    return Iterables.all( Topology.enabledServices( Storage.class ), new Predicate<ServiceConfiguration>( ) {
                      
                      @Override
                      public boolean apply( ServiceConfiguration arg0 ) {
                        if ( !arg0.getPartition( ).equals( sc.getPartition( ) ) ) {
                          try {
                            AsyncRequests.sendSync( arg0, deleteMsg );
                          } catch ( Exception ex ) {
                            LOG.error( ex );
                            Logs.extreme( ).error( ex, ex );
                          }
                        }
                        return true;
                      }
                    } );
                  }
                };
                Threads.enqueue( Eucalyptus.class, Snapshots.class, deleteBroadcast );
              } else {
                throw Exceptions.toUndeclared( "Unable to delete snapshot: " + snap, new EucalyptusCloudException( ) );
              }
            } catch ( Exception ex1 ) {
              throw Exceptions.toUndeclared( ex1.getMessage( ), ex1 );
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
    boolean showAll = request.getSnapshotSet( ).remove( "verbose" );
    AccountFullName ownerFullName = ( ctx.hasAdministrativePrivileges( ) && showAll ) ? null : AccountFullName.getInstance( ctx.getAccount( ) );
    EntityTransaction db = Entities.get( Snapshot.class );
    try {
      List<Snapshot> snapshots = Entities.query( Snapshot.named( ownerFullName, null ) );
      for ( Snapshot snap : Iterables.filter( snapshots, RestrictedTypes.filterPrivileged( ) ) ) {
        if ( request.getSnapshotSet( ).isEmpty( ) || request.getSnapshotSet( ).contains( snap.getDisplayName( ) ) ) {
          try {
            edu.ucsb.eucalyptus.msgs.Snapshot snapReply = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
            snapReply.setVolumeId( snap.getParentVolume( ) );
            snapReply.setOwnerId( snap.getOwnerAccountNumber( ) );
            reply.getSnapshotSet( ).add( snapReply );
          } catch ( NoSuchElementException e ) {
            LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
            LOG.debug( e, e );
          }
        }
      }
      db.rollback( );
    } catch ( Exception e ) {
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
