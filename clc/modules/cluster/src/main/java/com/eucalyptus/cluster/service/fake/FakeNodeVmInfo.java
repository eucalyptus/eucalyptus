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
package com.eucalyptus.cluster.service.fake;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.cluster.service.vm.VmVolumeAttachment;
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
  private final ConcurrentMap<String, VmVolumeAttachment> volumeAttachments = Maps.newConcurrentMap( );

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

  public ConcurrentMap<String, VmVolumeAttachment> getVolumeAttachments( ) {
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
