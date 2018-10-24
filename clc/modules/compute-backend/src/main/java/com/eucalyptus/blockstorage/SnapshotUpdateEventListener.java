/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.StorageSnapshot;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 *
 */
public class SnapshotUpdateEventListener implements EventListener<ClockTick>, Callable<Boolean> {
  private static final Logger LOG                     = Logger.getLogger( SnapshotUpdateEventListener.class );
  private static final long       SNAPSHOT_STATE_TIMEOUT  = 2 * 60 * 60 * 1000L;
  private static final Set<State> SNAPSHOT_TIMEOUT_STATES = unmodifiableSet(of(State.NIHIL, State.GENERATING));
  private static final AtomicBoolean ready = new AtomicBoolean( true );

  public static void register( ) {
    Listeners.register( ClockTick.class, new SnapshotUpdateEventListener() );
  }

  @Override
  public void fireEvent( ClockTick event ) {
    if ( Topology.isEnabledLocally( Eucalyptus.class ) && Topology.isEnabled( Storage.class ) &&
        Bootstrap.isOperational( ) && ready.compareAndSet( true, false ) ) {
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
        final Multimap<String, Snapshot> snapshots = ArrayListMultimap.create();
        for ( Snapshot s : Snapshots.list( ) ) {
          snapshots.put( s.getPartition( ), s );
        }
        final Map<String,Collection<Snapshot>> snapshotsByPartition = ImmutableMap.copyOf( snapshots.asMap( ) );
        final Map<String,Supplier<Map<String, StorageSnapshot>>> scSnapshotsByPartition = Maps.newHashMap( );
        for ( final String partition : snapshotsByPartition.keySet( ) ) {
          scSnapshotsByPartition.put( partition, getSnapshotsInPartition( partition ) );
        }
        for ( final String partition : snapshotsByPartition.keySet( ) ) {
          try {
            final Map<String, StorageSnapshot> storageSnapshots = scSnapshotsByPartition.get( partition ).get( );
            for ( final Snapshot snapshot : snapshotsByPartition.get( partition ) ) {
              final StorageSnapshot storageSnapshot = storageSnapshots.remove( snapshot.getDisplayName( ) );
              updateSnapshot( snapshot, storageSnapshot );
            }
            for ( StorageSnapshot unknownSnapshot : storageSnapshots.values( ) ) {
              LOG.trace( "SnapshotStateUpdate: found unknown snapshot: " + unknownSnapshot.getSnapshotId( ) + " " + unknownSnapshot.getStatus( ) );
            }
          } catch (Exception ex) {
            LOG.error( ex );
            Logs.extreme().error( ex, ex );
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

  private static Supplier<Map<String, StorageSnapshot>> getSnapshotsInPartition( final String partition ) {
    try {
      final ServiceConfiguration scConfig = Topology.lookup( Storage.class, Partitions.lookupByName( partition ) );
      final CheckedListenableFuture<DescribeStorageSnapshotsResponseType> describeFuture =
          AsyncRequests.dispatch( scConfig, new DescribeStorageSnapshotsType( ) );
      return new Supplier<Map<String, StorageSnapshot>>( ) {
        @Override
        public Map<String, StorageSnapshot> get() {
          final Map<String, StorageSnapshot> storageSnapshots = Maps.newHashMap( );
          try {
            final DescribeStorageSnapshotsResponseType snapshotInfo = describeFuture.get( );
            for ( final StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet( ) ) {
              storageSnapshots.put( storageSnapshot.getSnapshotId( ), storageSnapshot );
            }
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
          return storageSnapshots;
        }
      };
    } catch ( final NoSuchElementException ex ) {
      Logs.extreme( ).error( ex, ex );
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    return Suppliers.ofInstance( Collections.<String, StorageSnapshot>emptyMap( ) );
  }

  private static final class SnapshotStateChange {
             private final boolean timedOut;
    @Nonnull private final State newState; // could be the same as the old state
    @Nonnull private final Optional<String> progress;

    SnapshotStateChange(
                 final boolean timedOut,
        @Nonnull final State newState,
        @Nonnull final Optional<String> progress
    ) {
      this.timedOut = timedOut;
      this.newState = newState;
      this.progress = progress;
    }

    boolean willUpdate( final Snapshot entity ) {
      return timedOut ||
          newState != entity.getState( ) ||
          progress.isPresent( ) && !progress.get( ).equals( entity.getProgress( ) );
    }

    /**
     * Apply these changes to the given entity
     */
    void update( final Snapshot entity ) {
      if ( timedOut ) {
        Entities.delete( entity );
      } else {
        if ( progress.isPresent( ) ) {
          entity.setProgress( progress.get( ) );
        }

        entity.setState( newState );
      }
    }
  }

  /**
   * Determine changes to apply to entity, do not update entity here.
   */
  private static SnapshotStateChange changesFor( final Snapshot entity, final StorageSnapshot storageSnapshot ) {
    boolean timedOut = false;
    State newState = entity.getState( );
    Optional<String> progressUpdate = Optional.absent( );
    if ( storageSnapshot != null ) {
      if ( storageSnapshot.getStatus( ) != null ) {
        final Optional<State> stateUpdate = StorageUtil.mapState( storageSnapshot.getStatus( ) );
        if ( stateUpdate != null ) {
          newState = stateUpdate.get( );
        }
      }

      if ( !State.EXTANT.equals( newState ) && storageSnapshot.getProgress( ) != null ) {
        progressUpdate = Optional.of( storageSnapshot.getProgress( ) );
      } else if ( State.EXTANT.equals( newState ) ) {
        progressUpdate = Optional.of( "100%" );
      } else if ( State.GENERATING.equals( newState ) ) {
        if ( entity.getProgress( ) == null ) {
          progressUpdate = Optional.of( "0%" );
        }
      }
    } else if ( SNAPSHOT_TIMEOUT_STATES.contains( entity.getState( ) ) &&
        entity.lastUpdateMillis( ) > SNAPSHOT_STATE_TIMEOUT ) {
      timedOut = true;
    } else {
      if ( State.EXTANT.equals( entity.getState( ) ) ) {
        progressUpdate = Optional.of( "100%" );
      } else if ( State.GENERATING.equals( entity.getState( ) ) ) {
        if ( entity.getProgress( ) == null ) {
          progressUpdate = Optional.of( "0%" );
        }
      }
    }
    return new SnapshotStateChange( timedOut, newState, progressUpdate );
  }

  private static void updateSnapshot( final Snapshot snapshot, final StorageSnapshot storageSnapshot ) {
    try {
      final Function<String, Optional<SnapShotEvent>> updateSnapshot = new Function<String, Optional<SnapShotEvent>>( ) {
        public Optional<SnapShotEvent> apply( final String input ) {
          Optional<SnapShotEvent> event = Optional.absent( );
          try {
            final Snapshot entity = Entities.uniqueResult( Snapshot.named( null, input ) );
            final SnapshotStateChange stateChange = changesFor( entity, storageSnapshot );

            final StringBuilder buf = new StringBuilder( );
            buf.append( "SnapshotStateUpdate: " )
                .append( entity.getPartition( ) ).append( " " )
                .append( input ).append( " " )
                .append( entity.getParentVolume( ) ).append( " " )
                .append( entity.getState( ) ).append( " " )
                .append( entity.getProgress( ) ).append( " " );

            if ( entity.getState( ) == State.GENERATING && stateChange.newState == State.EXTANT  ) {
              //Went from GENERATING->EXTANT. Do the reporting event fire here.
              try {
                final Volume volume = Entities.uniqueResult( Volume.named( null, storageSnapshot.getVolumeId( ) ) );
                final String volumeUuid = volume.getNaturalId( );
                event = Optional.of( SnapShotEvent.with(
                    SnapShotEvent.forSnapShotCreate(
                        entity.getVolumeSize( ),
                        volumeUuid,
                        entity.getParentVolume( ) ),
                    entity.getNaturalId( ),
                    entity.getDisplayName( ),
                    entity.getOwnerUserId( ),
                    entity.getOwnerUserName( ),
                    entity.getOwnerAccountNumber( ) ) );
              } catch ( final Throwable e ) {
                LOG.error( "Error inserting/creating reporting event for snapshot creation of snapshot: " + entity.getDisplayName( ), e );
              }
            }

            // all changes to entity state must be via update
            stateChange.update( entity );

            if ( storageSnapshot != null ) {
              buf.append( " storage-snapshot " )
                  .append( storageSnapshot.getStatus( ) ).append( "=>" ).append( entity.getState( ) ).append( " " )
                  .append( storageSnapshot.getProgress( ) ).append( " " );
            }

            LOG.debug( buf.toString( ) );
            return event;
          } catch ( TransactionException ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      };
      if ( changesFor( snapshot, storageSnapshot ).willUpdate( snapshot ) ) {
        try {
          final Optional<SnapShotEvent> event =
              Entities.asTransaction( Snapshot.class, updateSnapshot ).apply( snapshot.getDisplayName( ) );
          //noinspection ConstantConditions
          if ( event.isPresent( ) ) {
            ListenerRegistry.getInstance( ).fireEvent( event.get( ) );
          }
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }
}
