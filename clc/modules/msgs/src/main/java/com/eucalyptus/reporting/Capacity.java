/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.Nonnull;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.vavr.collection.Stream;

/**
 * Track the latest capacity information from events preserving dimensions.
 */
public class Capacity {

  private static AtomicReference<CapacitySnapshot> capacitySnapshotRef =
      new AtomicReference<>( new CapacitySnapshot( Collections.emptyMap( ) ) );

  /**
   * Get the latest capacity snapshot.
   *
   * @return The snapshot which may have no capacity information but is never null
   */
  @Nonnull
  public static CapacitySnapshot snapshot( ) {
    return capacitySnapshotRef.get( );
  }

  /**
   * The known capacity at a given instant
   */
  public static class CapacitySnapshot {
    private final long timestamp;
    private final Map<String,Set<CapacityEntry>> capacityByType;

    public CapacitySnapshot( final long timestamp,
                             final Map<String, Set<CapacityEntry>> capacityByType ) {
      this.timestamp = timestamp;
      this.capacityByType = capacityByType;
    }

    public CapacitySnapshot( final Map<String, Set<CapacityEntry>> capacityByType ) {
      this( System.currentTimeMillis( ), capacityByType );
    }

    CapacitySnapshot update( final long timestamp,
                             final String type,
                             final Set<CapacityEntry> capacity ) {
      Assert.notNull( type, "type" );
      capacity.forEach( capacityEntry -> {
        if ( !type.equals( capacityEntry.getType( ) ) ) throw new IllegalArgumentException( "type mismatch" );
      } );
      final Map<String,Set<CapacityEntry>> newCapacityByType = Maps.newHashMap( );
      newCapacityByType.putAll( this.capacityByType );
      newCapacityByType.put( type, ImmutableSet.copyOf( capacity ) );
      return new CapacitySnapshot( timestamp, ImmutableMap.copyOf( newCapacityByType ) );
    }

    public long getTimestamp( ) {
      return timestamp;
    }

    public Stream<CapacityEntry> getCapacities( ) {
      return Stream.ofAll( capacityByType.values( ) ).flatMap( Function.<Iterable<CapacityEntry>>identity( ) );
    }

    @Override
    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "timestamp", Timestamps.formatIso8601Timestamp( new Date( timestamp ) ) )
          .add( "capacities", capacityByType.values( ) )
          .toString( );
    }
  }

  /**
   * Capacity values by subtypes and dimensions
   */
  public static class CapacityEntry {
    private final String type;
    private final Map<String,String> subtypes;
    private final Map<String,String> dimensions;
    private final long total;
    private final long available;

    public CapacityEntry(
        @Nonnull final String type,
        @Nonnull final Map<String, String> subtypes,
        @Nonnull final Map<String, String> dimensions,
                 final long total,
                 final long available
    ) {
      this.type = Assert.notNull( type, "type" );
      this.subtypes = ImmutableMap.copyOf( subtypes );
      this.dimensions = ImmutableMap.copyOf( dimensions );
      this.total = total;
      this.available = available;
    }

    public String getType( ) {
      return type;
    }

    public Map<String, String> getSubtypes( ) {
      return subtypes;
    }

    public Map<String, String> getDimensions( ) {
      return dimensions;
    }

    public long getTotal( ) {
      return total;
    }

    public long getAvailable( ) {
      return available;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final CapacityEntry that = (CapacityEntry) o;
      return Objects.equals( type, that.type ) &&
          Objects.equals( subtypes, that.subtypes ) &&
          Objects.equals( dimensions, that.dimensions );
    }

    @Override
    public int hashCode() {
      return Objects.hash( type, subtypes, dimensions );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "type", type )
          .add( "subtypes", subtypes )
          .add( "dimensions", dimensions )
          .add( "available", available )
          .add( "total", total )
          .toString( );
    }
  }

  public static class CapacityTrackingResourceAvailabilityEventListener implements EventListener<ResourceAvailabilityEvent> {
    public static void register( ) {
      Listeners.register( ResourceAvailabilityEvent.class, new CapacityTrackingResourceAvailabilityEventListener( ) );
    }

    @Override
    public void fireEvent( final ResourceAvailabilityEvent event ) {
      final String type = event.getType( ).name( );
      final Set<CapacityEntry> capacities = ImmutableSet.copyOf(
          Stream.ofAll( event.getAvailability( ) ).map( availability -> {
            final Map<String,String> types = Maps.newHashMap( );
            final Map<String,String> dimensions = Maps.newHashMap( );
            for ( final ResourceAvailabilityEvent.Tag tag : availability.getTags( ) ) {
              if ( tag instanceof ResourceAvailabilityEvent.Dimension ) {
                dimensions.put( tag.getType( ), tag.getValue( ) );
              } else {
                types.put( tag.getType( ), tag.getValue( ) );
              }
            }
            return new CapacityEntry(
                type,
                types,
                dimensions,
                availability.getTotal( ),
                availability.getAvailable( )
            );
          } )
      );

      for ( int i=0; i<10; i++ ) {
        final CapacitySnapshot currentSnapshot = capacitySnapshotRef.get( );
        if ( capacitySnapshotRef.compareAndSet(
            currentSnapshot,
            currentSnapshot.update( System.currentTimeMillis( ), type, capacities ) ) ) {
          break;
        }
      }
    }
  }
}
