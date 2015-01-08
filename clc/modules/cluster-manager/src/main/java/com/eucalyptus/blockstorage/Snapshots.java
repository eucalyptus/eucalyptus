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

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.StorageSnapshot;
import com.eucalyptus.compute.common.CloudMetadata.SnapshotMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.system.Threads;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class Snapshots {
  private static Logger           LOG                     = Logger.getLogger( Snapshots.class );
  private static final long       SNAPSHOT_STATE_TIMEOUT  = 2 * 60 * 60 * 1000L;
  private static final Set<State> SNAPSHOT_TIMEOUT_STATES = unmodifiableSet(of(State.NIHIL, State.GENERATING));
  
  public static class SnapshotUpdateEvent implements EventListener<ClockTick>, Callable<Boolean> {
    private static final AtomicBoolean ready = new AtomicBoolean( true );
    
    public static void register( ) {
      Listeners.register( ClockTick.class, new SnapshotUpdateEvent( ) );
    }
    
    @Override
    public void fireEvent( ClockTick event ) {
      if ( Topology.isEnabledLocally( Eucalyptus.class ) && ready.compareAndSet( true, false ) ) {
        try {
          Threads.enqueue( Eucalyptus.class, Snapshots.class, this );
        } catch ( Exception ex ) {
          ready.set( true );
        }
      }
    }
    
    @Override
    public Boolean call( ) throws Exception {
      try {
        try {
          Multimap<String, String> snapshots = ArrayListMultimap.create( );
          for ( Snapshot s : Snapshots.list( ) ) {
            snapshots.put( s.getPartition( ), s.getDisplayName( ) );
          }
          for ( final String partition : snapshots.keySet( ) ) {
        	try {
              ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( partition ) );
              DescribeStorageSnapshotsType scRequest = new DescribeStorageSnapshotsType( );
              DescribeStorageSnapshotsResponseType snapshotInfo = AsyncRequests.sendSync( sc, scRequest );
              final Map<String, StorageSnapshot> storageSnapshots = Maps.newHashMap( );
              for ( final StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet( ) ) {
                storageSnapshots.put( storageSnapshot.getSnapshotId( ), storageSnapshot );
              }
              for ( String snapshotId : snapshots.get( partition ) ) {
                final StorageSnapshot storageSnapshot = storageSnapshots.remove( snapshotId );
                updateSnapshot( snapshotId, storageSnapshot );
              }
              for ( StorageSnapshot unknownSnapshot : storageSnapshots.values( ) ) {
                LOG.trace( "SnapshotStateUpdate: found unknown snapshot: " + unknownSnapshot.getSnapshotId( ) + " " + unknownSnapshot.getStatus( ) );
              }
            } catch (Exception ex) {
              LOG.error( ex );
              Logs.extreme( ).error( ex, ex );
            }
          }
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
      } finally {
        ready.set( true );
      }
      return true;
    }
    
    public void updateSnapshot( String snapshotId, final StorageSnapshot storageSnapshot ) {
      try {
        final Function<String, Snapshot> updateSnapshot = new Function<String, Snapshot>( ) {
          public Snapshot apply( final String input ) {
            try {
              Snapshot entity = Entities.uniqueResult( Snapshot.named( null, input ) );
              StringBuilder buf = new StringBuilder( );
              buf.append( "SnapshotStateUpdate: " )
                   .append( entity.getPartition( ) ).append( " " )
                   .append( input ).append( " " )
                   .append( entity.getParentVolume( ) ).append( " " )
                   .append( entity.getState( ) ).append( " " )
                   .append( entity.getProgress( ) ).append( " " );
              if ( storageSnapshot != null ) {
                           
            	if ( storageSnapshot.getStatus( ) != null ) {
            		boolean wasGenerating = entity.getState().equals(State.GENERATING); 
            		entity.setMappedState( storageSnapshot.getStatus( ) );
            		if(wasGenerating && entity.getState().equals(State.EXTANT)) {
            			//Went from GENERATING->EXTANT. Do the reporting event fire here.
            			try {
    						final Volume volume = Transactions.find( Volume.named( null, storageSnapshot.getVolumeId()) );
    						final String volumeUuid = volume.getNaturalId();
    						ListenerRegistry.getInstance().fireEvent( SnapShotEvent.with(
    								SnapShotEvent.forSnapShotCreate(
    										entity.getVolumeSize(),
    										volumeUuid, 
    										entity.getParentVolume()),
    										entity.getNaturalId(),
    										entity.getDisplayName(),
    										entity.getOwnerUserId() ) ); // snapshot info user name is user id
    					} catch ( final Throwable e ) {
    						LOG.error( "Error inserting/creating reporting event for snapshot creation of snapshot: " + entity.getDisplayName(), e  );
    					}
            		}
                }
                
                if ( !State.EXTANT.equals( entity.getState( ) ) && storageSnapshot.getProgress( ) != null ) {
                  entity.setProgress( storageSnapshot.getProgress( ) );
                } else if ( State.EXTANT.equals( entity.getState( ) ) ) {
                  entity.setProgress( "100%" );
                } else if ( State.GENERATING.equals( entity.getState( ) ) ) {
                  if ( entity.getProgress( ) == null ) {
                    entity.setProgress( "0%" );
                  }
                }
                buf.append( " storage-snapshot " )
                   .append( storageSnapshot.getStatus( ) ).append( "=>" ).append( entity.getState( ) ).append( " " )
                   .append( storageSnapshot.getProgress( ) ).append( " " );
              } else if ( SNAPSHOT_TIMEOUT_STATES.contains( entity.getState( ) ) &&
                  entity.lastUpdateMillis( ) > SNAPSHOT_STATE_TIMEOUT ) {
                Entities.delete( entity );
              } else {
                if ( State.EXTANT.equals( entity.getState( ) ) ) {
                  entity.setProgress( "100%" );
                } else if ( State.GENERATING.equals( entity.getState( ) ) ) {
                  if ( entity.getProgress( ) == null ) {
                    entity.setProgress( "0%" );
                  }
                }
              }
              LOG.debug( buf.toString( ) );
              return entity;
            } catch ( TransactionException ex ) {
              throw Exceptions.toUndeclared( ex );
            }
          }
        };
        try {
          Entities.asTransaction( Snapshot.class, updateSnapshot ).apply( snapshotId );
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }
  
  @QuantityMetricFunction( SnapshotMetadata.class )
  public enum CountSnapshots implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( OwnerFullName input ) {
      final EntityTransaction db = Entities.get( Snapshot.class );
      try {
        return Entities.count( Snapshot.named( input, null ) );
      } finally {
        db.rollback( );
      }
    }
  }
  
  static Snapshot initializeSnapshot( final UserFullName userFullName,
                                      final Volume vol,
                                      final ServiceConfiguration sc,
                                      final String description ) throws EucalyptusCloudException {
    final EntityTransaction db = Entities.get( Snapshot.class );
    try {
      while ( true ) {
        final String newId = ResourceIdentifiers.generateString( SnapshotManager.ID_PREFIX );
        try {
          Entities.uniqueResult( Snapshot.named( null, newId ) );
        } catch ( NoSuchElementException e ) {
          final Snapshot snap = new Snapshot( userFullName, newId, description, vol.getDisplayName( ), vol.getSize( ), sc.getName( ), sc.getPartition( ) );
          Entities.persist( snap );
          db.commit( );
          return snap;
        }
      }
    } catch ( Exception ex ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to initialize snapshot state because of: " + ex.getMessage( ), ex );
    }
  }
  
  static Snapshot startCreateSnapshot( final Volume vol, final Snapshot snap ) throws EucalyptusCloudException, DuplicateMetadataException {
    final ServiceConfiguration sc = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
    try {
      Snapshot snapState = Transactions.save( snap, new Callback<Snapshot>( ) {
        
        @Override
        public void fire( Snapshot s ) {
          String scSnapStatus = null;
        	try {
          	CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName( ), snap.getDisplayName( ) );
            CreateStorageSnapshotResponseType scReply = AsyncRequests.sendSync( sc, scRequest );
            s.setMappedState( scReply.getStatus( ) );
            scSnapStatus = scReply.getStatus();
          } catch ( Exception ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      } );      
    } catch ( ConstraintViolationException ex ) {
      throw new DuplicateMetadataException( "Duplicate snapshot creation: " + snap + ": " + ex.getMessage( ), ex );
    } catch ( ExecutionException ex ) {
      LOG.error( ex.getCause( ), ex.getCause( ) );
      throw new EucalyptusCloudException( ex );
    }
    
    return snap;
  }
  
  public static Snapshot named( final String snapshotId ) {
    return new Snapshot( null, snapshotId );
  }
  
  public static Snapshot lookup( @Nullable OwnerFullName accountFullName, String snapshotId ) throws ExecutionException {
    return Transactions.find(Snapshot.named(accountFullName, snapshotId));
  }
  
  public static List<Snapshot> list( ) throws TransactionException {
    return Transactions.findAll( Snapshot.named( null, null ) );
  }

  public static class SnapshotFilterSupport extends FilterSupport<Snapshot> {
    public SnapshotFilterSupport() {
      super( builderFor( Snapshot.class )
          .withTagFiltering( SnapshotTag.class, "snapshot" )
          .withStringProperty( "description", FilterFunctions.DESCRIPTION )
          .withLikeExplodedProperty( "owner-alias", FilterFunctions.ACCOUNT_ID, accountAliasExploder() )
          .withStringProperty( "owner-id", FilterFunctions.ACCOUNT_ID )
          .withStringProperty( "progress", FilterFunctions.PROGRESS )
          .withStringProperty( "snapshot-id", CloudMetadatas.toDisplayName() )
          .withStringProperty( "status", FilterFunctions.STATUS )
          .withDateProperty( "start-time", FilterDateFunctions.START_TIME )
          .withStringProperty( "volume-id", FilterFunctions.VOLUME_ID )
          .withStringProperty( "volume-size", FilterFunctions.VOLUME_SIZE )
          .withPersistenceFilter( "description" )
          .withLikeExplodingPersistenceFilter( "owner-alias", "ownerAccountNumber", accountAliasExploder() )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "progress" )
          .withPersistenceFilter( "snapshot-id", "displayName" )
          .withPersistenceFilter( "start-time", "creationTimestamp", PersistenceFilter.Type.Date )
          .withPersistenceFilter( "volume-id", "parentVolume" )
          .withPersistenceFilter( "volume-size", "volumeSize", PersistenceFilter.Type.Integer )
      );
    }
  }

  private static Function<String,Collection> accountAliasExploder() {
    return new Function<String,Collection>() {
      @Override
      public Collection<String> apply( final String accountAliasExpression ) {
        try {
          return Accounts.resolveAccountNumbersForName( accountAliasExpression );
        } catch ( AuthException e ) {
          LOG.error( e, e );
          return Collections.emptySet();
        }
      }
    };
  }  

  private enum FilterFunctions implements Function<Snapshot,String> {
    DESCRIPTION {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getDescription();
      }
    },
    ACCOUNT_ID {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getOwnerAccountNumber();
      }
    },
    PROGRESS {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getProgress();
      }
    },
    STATUS {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.mapState();
      }
    },
    VOLUME_ID {
      @Override
      public String apply( final Snapshot snapshot ) {
        return snapshot.getParentVolume();
      }
    },
    VOLUME_SIZE {
      @Override
      public String apply( final Snapshot snapshot ) {
        Integer size = snapshot.getVolumeSize();
        return size == null ? null : String.valueOf( size );
      }
    }
  }

  private enum FilterDateFunctions implements Function<Snapshot,Date> {
    START_TIME {
      @Override
      public Date apply( final Snapshot snapshot ) {
        return snapshot.getCreationTimestamp();
      }
    }
  }
}
