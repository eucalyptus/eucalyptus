/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.vm;

import java.util.Date;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import io.vavr.control.Option;

/**
 *
 */
public class ClusterVmMigrationState {
  private static final ClusterVmMigrationState NONE =
      new ClusterVmMigrationState( Option.none( ), "none", null, null );

  private final Option<Long> stateTimestamp;
  private final String state;
  private final String sourceHost;
  private final String destinationHost;

  private ClusterVmMigrationState(
      final Option<Long> stateTimestamp,
      final String state,
      final String sourceHost,
      final String destinationHost
  ) {
    this.stateTimestamp = Assert.notNull( stateTimestamp, "stateTimestamp" );
    this.state = Assert.notNull( state, "state" );
    this.sourceHost = sourceHost;
    this.destinationHost = destinationHost;
  }

  @Nonnull
  public Option<Long> getStateTimestamp( ) {
    return stateTimestamp;
  }

  @Nonnull
  public String getState( ) {
    return state;
  }

  @Nullable
  public String getSourceHost( ) {
    return sourceHost;
  }

  @Nullable
  public String getDestinationHost( ) {
    return destinationHost;
  }

  public static ClusterVmMigrationState none( ) {
    return NONE;
  }

  public static ClusterVmMigrationState of(
      final Option<Long> stateTimestamp,
      final String state,
      final String srcHost,
      final String dstHost
  ) {
    final ClusterVmMigrationState info =
        new ClusterVmMigrationState( stateTimestamp, state, srcHost, dstHost );
    return info.equals( none( ) ) ? none( ) : info;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Option<Long> timeForState( final String state, final long time ) {
    return none( ).getState( ).equals( state ) ?
        Option.none( ) :
        Option.of( time );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ClusterVmMigrationState info = (ClusterVmMigrationState) o;
    return Objects.equals( getState( ), info.getState( ) ) &&
        Objects.equals( getSourceHost( ), info.getSourceHost( ) ) &&
        Objects.equals( getDestinationHost( ), info.getDestinationHost( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getState( ), getSourceHost( ), getDestinationHost( ) );
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper( this )
        .omitNullValues( )
        .add( "state", getState( ) )
        .add( "state-timestamp", getStateTimestamp( )
            .map( t -> Timestamps.formatIso8601Timestamp( new Date( t ) ) ).getOrElse( (String)null ) )
        .add( "source-host", getSourceHost( ) )
        .add( "destination-host", getDestinationHost( ) )
        .toString( );
  }
}
