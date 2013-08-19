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

package com.eucalyptus.blockstorage;

import static com.eucalyptus.cloud.ImageMetadata.State.available;
import static com.eucalyptus.cloud.ImageMetadata.State.pending;
import static com.eucalyptus.images.Images.inState;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotType;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.component.NoSuchComponentException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.images.Images;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.EventActionInfo;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event.SnapShotEvent.SnapShotAction;
import com.eucalyptus.system.Threads;
import com.eucalyptus.tags.Filter;
import com.eucalyptus.tags.Filters;
import com.eucalyptus.tags.Tag;
import com.eucalyptus.tags.TagSupport;
import com.eucalyptus.tags.Tags;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import edu.ucsb.eucalyptus.msgs.CreateSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifySnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetSnapshotAttributeType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;

public class SnapshotManager {
  
  static Logger LOG       = Logger.getLogger( SnapshotManager.class );
  static String ID_PREFIX = "snap";
  
  public CreateSnapshotResponseType create( final CreateSnapshotType request ) throws EucalyptusCloudException, NoSuchComponentException, DuplicateMetadataException, AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException, TransactionException {
    final Context ctx = Contexts.lookup( );
    Volume vol = Transactions.find( Volume.named( ctx.getUserFullName( ).asAccountFullName( ), request.getVolumeId( ) ) );
    final ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
    final Volume volReady = Volumes.checkVolumeReady( vol );
    Supplier<Snapshot> allocator = new Supplier<Snapshot>( ) {
      
      @Override
      public Snapshot get( ) {
        try {
          return Snapshots.initializeSnapshot( ctx.getUserFullName( ), volReady, sc, request.getDescription() );
        } catch ( EucalyptusCloudException ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    Snapshot snap = RestrictedTypes.allocateUnitlessResource( allocator );
    try {
      snap = Snapshots.startCreateSnapshot( volReady, snap );
    } catch ( EucalyptusCloudException e ) {
      final EntityTransaction db = Entities.get( Snapshot.class );
      try {
        Snapshot entity = Entities.uniqueResult( snap );
        Entities.delete( entity );
        db.commit();
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex , ex );
      } finally {
        if ( db.isActive() ) db.rollback( );
      }
      throw e;
    }

    try {
      fireUsageEvent( snap, SnapShotEvent.forSnapShotCreate( snap.getVolumeSize(), volReady.getNaturalId(), snap.getDisplayName() ) );
    } catch (Throwable reportEx) {
      LOG.error("Unable to fire snap shot creation reporting event", reportEx);
    }
    
    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply( );
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
    snapMsg.setProgress( "0%" );
    snapMsg.setOwnerId( snap.getOwnerAccountNumber( ) );
    snapMsg.setVolumeSize( volReady.getSize( ).toString( ) );
    reply.setSnapshot( snapMsg );
    return reply;
  }

  public DeleteSnapshotResponseType delete( final DeleteSnapshotType request ) throws EucalyptusCloudException {
    final DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    Predicate<Snapshot> deleteSnapshot = new Predicate<Snapshot>( ) {
      
      @Override
      public boolean apply( Snapshot snap ) {
        if ( !State.EXTANT.equals( snap.getState( ) ) && !State.FAIL.equals( snap.getState( ) ) ) {
          return false;
        } else if ( !RestrictedTypes.filterPrivileged( ).apply( snap ) ) {
          throw Exceptions.toUndeclared( "Not authorized to delete snapshot " + request.getSnapshotId( ) + " by " + ctx.getUser( ).getName( ),
                                         new EucalyptusCloudException( ) );
        } else if ( isReservedSnapshot( request.getSnapshotId( ) ) ) {
          throw Exceptions.toUndeclared( "Snapshot " + request.getSnapshotId( ) + " is in use, deletion not permitted", new EucalyptusCloudException( ) );
        } else {
          fireUsageEvent(snap, SnapShotEvent.forSnapShotDelete());
          
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
    };
    boolean result = false;
    try {
      result = Transactions.delete( Snapshot.named( ctx.getUserFullName( ).asAccountFullName( ), request.getSnapshotId( ) ), deleteSnapshot);
    } catch ( NoSuchElementException ex2 ) {
      try {
        result = Transactions.delete( Snapshot.named( null, request.getSnapshotId( ) ), deleteSnapshot);
      } catch ( ExecutionException ex3 ) {
        throw new EucalyptusCloudException( ex3.getCause( ) );
      } catch ( NoSuchElementException ex4 ) {
      }
    } catch ( ExecutionException ex1 ) {
      throw new EucalyptusCloudException( ex1.getCause( ) );
    }
    reply.set_return( result );
    return reply;
  }
  
  public DescribeSnapshotsResponseType describe( final DescribeSnapshotsType request ) throws EucalyptusCloudException {
    final DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply( );
    if(!request.getRestorableBySet().isEmpty()) { return reply; } //TODO:KEN EUCA-5759 Need to implement RestorableBy, for now ignore
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.getSnapshotSet( ).remove( "verbose" );
    final AccountFullName ownerFullName = ( ctx.hasAdministrativePrivileges( ) && showAll ) ?
        null :
        AccountFullName.getInstance( ctx.getAccount( ) );
    final Filter filter = Filters.generate( request.getFilterSet(), Snapshot.class );
    final EntityTransaction db = Entities.get( Snapshot.class );
    try {
      final List<Snapshot> unfilteredSnapshots =
          Entities.query( Snapshot.named( ownerFullName, null ), true, filter.asCriterion(), filter.getAliases() );
      final Predicate<? super Snapshot> requestedAndAccessible = CloudMetadatas.filteringFor(Snapshot.class)
          .byId( request.getSnapshotSet() )
          .byPredicate( filter.asPredicate() )
          .byPrivileges()
          .buildPredicate();

      final Iterable<Snapshot> snapshots = Iterables.filter( unfilteredSnapshots, requestedAndAccessible );
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( Snapshot.class )
          .getResourceTagMap( AccountFullName.getInstance( ctx.getAccount( ) ),
              Iterables.transform( snapshots, CloudMetadatas.toDisplayName() ) );
      for ( final Snapshot snap : snapshots ) {
        try {
          final edu.ucsb.eucalyptus.msgs.Snapshot snapReply = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot( ) );
          Tags.addFromTags( snapReply.getTagSet(), ResourceTag.class, tagsMap.get( snapReply.getSnapshotId() ) );
          snapReply.setVolumeId( snap.getParentVolume( ) );
          snapReply.setOwnerId( snap.getOwnerAccountNumber( ) );
          reply.getSnapshotSet( ).add( snapReply );
        } catch ( NoSuchElementException e ) {
          LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
          LOG.debug( e, e );
        }
      }
    } finally {
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

  private static boolean isReservedSnapshot( final String snapshotId ) {
	// Fix for EUCA-4932. Any snapshot associated with an (available or pending) image as a root/non-root device is a reserved snapshot
	// and can't be deleted without first unregistering the image
    return Predicates.or( SnapshotInUseVerifier.INSTANCE ).apply( snapshotId );
  }

  private enum ImageSnapshotReservation implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply( final String identifier ) {
      return Iterables.any(
          Entities.query(Images.exampleBlockStorageWithSnapshotId(identifier), true),
          inState(EnumSet.of( pending, available ) ) );
    }
  }
  
  /**
   * <p>Predicate to check if a snapshot is associated with a pending or available boot from ebs image.
   * Returns true if the snapshot is used in the image registration with root or non root devices.</p>
   */
  private enum SnapshotInUseVerifier implements Predicate<String> {
	INSTANCE;

	@Override
	public boolean apply( final String identifier ) {
	  return Iterables.any(
	    Entities.query(Images.exampleBSDMappingWithSnapshotId(identifier), true),
	    Images.imageInState(EnumSet.of( pending, available ) ) );
	}
  }
  
  private static void fireUsageEvent( final Snapshot snap,
                                      final EventActionInfo<SnapShotAction> actionInfo ) {
    try {
      ListenerRegistry.getInstance().fireEvent(
          SnapShotEvent.with(actionInfo, snap.getNaturalId(), snap.getDisplayName(), snap.getOwner().getUserId() ));
    } catch (final Throwable e) {
      LOG.error(e, e);
    }
  }
}
