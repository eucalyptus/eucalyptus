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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
*
*/
public class VolumeUpdateEventListener implements EventListener<ClockTick>, Callable<Boolean> {
  private static final Logger LOG                 = Logger.getLogger( VolumeUpdateEventListener.class );
  private static final long VOLUME_STATE_TIMEOUT  = 2 * 60 * 60 * 1000L;
  private static final AtomicBoolean ready = new AtomicBoolean( true );
  private static final ConcurrentMap<String, Long> pendingUpdates = Maps.newConcurrentMap( );

  public static void register( ) {
    Listeners.register( ClockTick.class, new VolumeUpdateEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Topology.isEnabledLocally( Eucalyptus.class ) && Topology.isEnabled( Storage.class ) &&
        Bootstrap.isOperational( ) && ready.compareAndSet( true, false ) ) {
      try {
        Threads.enqueue( Eucalyptus.class, Volumes.class, this );
      } catch ( final Exception ex ) {
        ready.set( true );
      }
    }
  }

  @Override
  public Boolean call( ) throws Exception {
    try {
      VolumeUpdateEventListener.update();
    } finally {
      ready.set( true );
    }
    return true;
  }

  static void update( ) {
    final Multimap<String, Volume> partitionVolumeMap = HashMultimap.create( );
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( Volume.class ) ) {
      for ( final Volume v : Entities.query( Volume.named( null, null ) ) ) {
        partitionVolumeMap.put( v.getPartition( ).intern( ), v );
      }
    } catch ( final Exception ex ) {
      Logs.extreme().error( ex, ex );
    }
    final Map<String,Collection<Volume>> volumesByPartition = ImmutableMap.copyOf( partitionVolumeMap.asMap( ) );
    final Map<String,Supplier<Map<String, StorageVolume>>> scVolumesByPartition = Maps.newHashMap( );
    for ( final String partition : volumesByPartition.keySet( ) ) {
      scVolumesByPartition.put( partition, updateVolumesInPartition( partition ) );//TODO:GRZE: restoring volume state
    }
    for ( final String partition : volumesByPartition.keySet( ) ) {
      try {
        final Map<String, StorageVolume> idStorageVolumeMap = scVolumesByPartition.get( partition ).get( );
        for ( final Volume v : volumesByPartition.get( partition ) ) {
          try {
            final StorageVolume storageVolume = idStorageVolumeMap.get( v.getDisplayName( ) );
            if ( pendingUpdates.putIfAbsent( v.getDisplayName( ), System.currentTimeMillis( ) ) == null ) try {
              Threads.enqueue(
                  Storage.class,
                  VolumeUpdateEventListener.class,
                  ( Runtime.getRuntime( ).availableProcessors( ) * 2 ) + 1,
                  new Callable<Void>( ) {
                    @Override
                    public Void call() throws Exception {
                      try {
                        volumeStateUpdate( v, storageVolume );
                      } finally {
                        pendingUpdates.remove( v.getDisplayName( ) );
                      }
                      return null;
                    }
                  }
              );
            } catch ( Throwable t ) {
              pendingUpdates.remove( v.getDisplayName( ) );
              throw Throwables.propagate( t );
            }
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }

  static void volumeStateUpdate( final Volume volume, final StorageVolume storageVolume ) {
    final StringBuilder buf = new StringBuilder( );
    final Function<String, Volume> updateVolume = new Function<String, Volume>( ) {
      @Override
      public Volume apply( final String input ) {
        try {
          final Volume volumeToUpdate = Entities.uniqueResult( Volume.named( null, input ) );
          State volumeState = volumeToUpdate.getState( );
          Integer size = 0;
          final Optional<State> newState =
              calculateState( volumeToUpdate, storageVolume==null ? null : storageVolume.getStatus( ) );
          if ( storageVolume != null ) {
            size = Integer.parseInt( storageVolume.getSize( ) );
          }
          if ( newState.isPresent( ) ) {
            volumeState = newState.get( );
          }
          volumeToUpdate.setState( volumeState );
          try {
            if ( volumeToUpdate.getSize( ) <= 0 ) {
              volumeToUpdate.setSize( size );
              if ( EnumSet.of( State.GENERATING, State.EXTANT, State.BUSY ).contains( volumeState ) ) {
                Volumes.fireUsageEvent( volumeToUpdate, VolumeEvent.forVolumeCreate() );
              }
            }
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
          //TODO:GRZE: expire deleted/failed volumes in the future.
          //            if ( State.ANNIHILATED.equals( v.getState( ) ) && State.ANNIHILATED.equals( v.getState( ) ) && v.lastUpdateMillis( ) > VOLUME_DELETE_TIMEOUT ) {
          //              Entities.delete( v );
          //            }
          buf.append( " Resulting new-state: [" ).append( volumeToUpdate.getState( ) ).append("]");
          LOG.debug( buf.toString( ) );
          return volumeToUpdate;
        } catch ( final TransactionException ex ) {
          LOG.error( buf.toString( ) + " failed because of " + ex.getMessage( ) );
          Logs.extreme( ).error( buf.toString( ) + " failed because of " + ex.getMessage( ), ex );
          throw Exceptions.toUndeclared( ex );
        } catch ( final NoSuchElementException ex ) {
          LOG.error( buf.toString( ) + " failed because of " + ex.getMessage( ) );
          Logs.extreme( ).error( buf.toString( ) + " failed because of " + ex.getMessage( ), ex );
          throw ex;
        }
      }
    };

    final Optional<State> newState = calculateState( volume, storageVolume==null ? null : storageVolume.getStatus( ) );

    buf.append( "VolumeStateUpdate: Current Volume Info: [" )
        .append( "Partition: ").append(volume.getPartition( ) ).append( " " )
        .append( "Name: ").append(volume.getDisplayName( ) ).append( " " )
        .append( "CurrentState: ").append(volume.getState( ) ).append( " " )
        .append( "Created: ").append(volume.getCreationTimestamp( ) ).append(" ]");
    if ( storageVolume != null ) {
      buf.append( " Incoming state update: [" )
          .append("State: ").append( storageVolume.getStatus( ) ).append( "=>" ).append( newState ).append( " " )
          .append("Size: ").append( storageVolume.getSize( ) ).append( "GB " )
          .append("SourceSnapshotId: ").append( storageVolume.getSnapshotId( ) ).append( " " )
          .append("CreationTime: ").append( storageVolume.getCreateTime( ) ).append( " " )
          .append("DeviceName: ").append( storageVolume.getActualDeviceName( ) ).append(" ] ");
    }

    if ( volume.getSize( ) <= 0 ||
        ( newState.isPresent( ) && newState.get( ) != volume.getState( ) ) ) {
      Entities.asTransaction( Volume.class, updateVolume ).apply( volume.getDisplayName( ) );
    } else {
      LOG.debug( buf.toString( ) + " unchanged" );
    }
  }

  static Optional<State> calculateState( final Volume volumeToUpdate, final String storageStatus ) {
    Optional<State> state = Optional.of( volumeToUpdate.getState( ) );
    if ( storageStatus != null ) {
      String status = storageStatus;
      //NOTE: removed this conditional check for initial state and actual device name. An empty actualDeviceName or 'invalid'
      //is legitimate if the volume is not exported/attached. Only on attachment request will device name be populated
      //if ( State.EXTANT.equals( initialState )
      //       && ( ( actualDeviceName == null ) || "invalid".equals( actualDeviceName ) || "unknown".equals( actualDeviceName ) ) ) {
      //
      //  volumeState = State.GENERATING;
      //} else if ( State.ANNIHILATING.equals( initialState ) && State.ANNIHILATED.equals( Volumes.transformStorageState( v.getState( ), status ) ) ) {
      if ( State.ANNIHILATING == volumeToUpdate.getState( )  && State.ANNIHILATED == Volumes.transformStorageState( volumeToUpdate.getState( ), status ) ) {
        state = Optional.of( State.ANNIHILATED );
      } else {
        state = Optional.of( Volumes.transformStorageState( volumeToUpdate.getState( ), status ) );
      }
    } else if ( State.ANNIHILATING.equals( volumeToUpdate.getState( ) ) ) {
      state = Optional.of( State.ANNIHILATED );
    } else if ( State.GENERATING.equals( volumeToUpdate.getState( ) ) && volumeToUpdate.lastUpdateMillis( ) > VOLUME_STATE_TIMEOUT ) {
      state = Optional.of( State.FAIL );
    } else if ( State.EXTANT.equals( volumeToUpdate.getState( ) ) && volumeToUpdate.lastUpdateMillis( ) > VOLUME_STATE_TIMEOUT ) {
      //volume is available but the SC does not know about it.
      //This is based on a guarantee that the SC will never send partial information
      //If the SC subsequently reports it as available, it will be recovered
      state = Optional.of( State.ERROR );
    }
    return state;
  }

  static Supplier<Map<String, StorageVolume>> updateVolumesInPartition( final String partition ) {
    try {
      final ServiceConfiguration scConfig = Topology.lookup( Storage.class, Partitions.lookupByName( partition ) );
      final CheckedListenableFuture<DescribeStorageVolumesResponseType> describeFuture =
          AsyncRequests.dispatch( scConfig, new DescribeStorageVolumesType( ) );
      return new Supplier<Map<String, StorageVolume>>( ) {
        @Override
        public Map<String, StorageVolume> get( ) {
          final Map<String, StorageVolume> idStorageVolumeMap = Maps.newHashMap();

          try {
            final DescribeStorageVolumesResponseType volState = describeFuture.get( );
            for ( final StorageVolume vol : volState.getVolumeSet( ) ) {
              LOG.trace( "Volume states: " + vol.getVolumeId( ) + " " + vol.getStatus( ) + " " + vol.getActualDeviceName( ) );
              idStorageVolumeMap.put( vol.getVolumeId( ), vol );
            }
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }

          return idStorageVolumeMap;
        }
      };
    } catch ( final NoSuchElementException ex ) {
      Logs.extreme( ).error( ex, ex );
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    return Suppliers.ofInstance( Collections.<String, StorageVolume>emptyMap( ) );
  }

  private static final class VolumeUpdateTaskExpiryEventListener implements EventListener<ClockTick> {
    public static void register( ){
      Listeners.register( ClockTick.class, new VolumeUpdateTaskExpiryEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      final long expiry = System.currentTimeMillis( ) - TimeUnit.MINUTES.toMillis( 5 );
      for ( final Map.Entry<String,Long> entry : pendingUpdates.entrySet( ) ) {
        if ( entry.getValue( ) < expiry ) {
          if ( pendingUpdates.remove( entry.getKey( ), entry.getValue( ) ) ) {
            LOG.warn( "Expired update task for volume " + entry.getKey( ) );
          }
        }
      }
    }
  }
}
