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

import java.util.Objects;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class ClusterVmBundleState {
  private static final String NONE_STATE = "none";
  private static final ClusterVmBundleState NONE = new ClusterVmBundleState( NONE_STATE, null );

  private final String state;
  private final Double progress;

  private ClusterVmBundleState( final String state, final Double progress ) {
    this.state = Assert.notNull( state, "state" );
    this.progress = NONE_STATE.equals( this.state ) ? null :
        Math.max( 0d, Math.min( 1d, MoreObjects.firstNonNull( progress, 0d ) ) );
  }

  public static ClusterVmBundleState none( ) {
    return NONE;
  }

  public static ClusterVmBundleState of( final String state, final Double progress ) {
    final ClusterVmBundleState info = new ClusterVmBundleState( state, progress );
    return info.equals( none( ) ) ? none( ) : info;
  }

  @Nonnull
  public String getState( ) {
    return state;
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public Double getProgress( ) {
    return MoreObjects.firstNonNull( progress, 0D );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ClusterVmBundleState that = (ClusterVmBundleState) o;
    return Objects.equals( getState( ), that.getState( ) ) &&
        Objects.equals( getProgress( ), that.getProgress( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getState( ), getProgress( ) );
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper( this )
        .omitNullValues( )
        .add( "state", state )
        .add( "progress", progress )
        .toString( );
  }
}
