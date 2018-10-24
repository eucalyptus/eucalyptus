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
package com.eucalyptus.cluster.service.fake;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.cluster.service.vm.ClusterVmVolume;
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 *
 */
public class FakeNodeVmInfo {
  public enum State { Extant, Teardown }

  private final String id;
  private volatile State state;
  private volatile long stateTimestamp;
  private volatile String publicIp;
  private final ConcurrentMap<String, ClusterVmVolume> volumeAttachments = Maps.newConcurrentMap( );

  public FakeNodeVmInfo( final String id ) {
    this.id = id;
  }

  public String getId( ) {
    return id;
  }

  public State getState( ) {
    return state;
  }

  public void setState( final State state ) {
    this.state = state;
  }

  public long getStateTimestamp( ) {
    return stateTimestamp;
  }

  public void setStateTimestamp( final long stateTimestamp ) {
    this.stateTimestamp = stateTimestamp;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( final String publicIp ) {
    this.publicIp = publicIp;
  }

  public String getStateName( ) {
    return getState( ).name( );
  }

  public ConcurrentMap<String, ClusterVmVolume> getVolumeAttachments( ) {
    return volumeAttachments;
  }

  public void state( long stateTimestamp, State state ) {
    if ( getState( ) == null || state.ordinal( ) > getState( ).ordinal( ) ) {
      setState( state );
      setStateTimestamp( stateTimestamp );
    }
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "id", getId( ) )
        .add( "state", getState( ) )
        .add( "state-timestamp", Timestamps.formatIso8601Timestamp( new Date( getStateTimestamp( ) ) ) )
        .add( "public-ip", getPublicIp( ) )
        .omitNullValues( )
        .toString( );
  }
}
