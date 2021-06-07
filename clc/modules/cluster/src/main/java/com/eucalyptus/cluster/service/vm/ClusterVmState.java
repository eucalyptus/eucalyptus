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
