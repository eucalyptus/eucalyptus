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
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

/**
 *
 */
public class ClusterVmState {

  private final long timestamp;
  private final String state;
  private final String guestState;

  private ClusterVmState(
      final long timestamp,
      final String state,
      final String guestState
  ) {
    this.timestamp = timestamp;
    this.state = state;
    this.guestState = Strings.emptyToNull( guestState );
  }

  public static ClusterVmState of(
      final long timestamp,
      final String state,
      final String guestState
  ) {
    return new ClusterVmState( timestamp, state, guestState );
  }

  @SuppressWarnings( "WeakerAccess" )
  public long getTimestamp( ) {
    return timestamp;
  }

  public String getState( ) {
    return state;
  }

  @SuppressWarnings( "WeakerAccess" )
  public String getGuestState( ) {
    return guestState;
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .omitNullValues( )
        .add( "timestamp", Timestamps.formatIso8601Timestamp( new Date( getTimestamp( ) ) ) )
        .add( "state", getState( ) )
        .add( "guest-state", getGuestState( ) )
        .toString( );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ClusterVmState that = (ClusterVmState) o;
    return Objects.equals( getState( ), that.getState( ) ) &&
        Objects.equals( getGuestState( ), that.getGuestState( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getState( ), getGuestState( ) );
  }
}
