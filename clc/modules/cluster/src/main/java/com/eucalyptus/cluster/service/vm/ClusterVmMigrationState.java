/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
