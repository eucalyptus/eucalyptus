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
