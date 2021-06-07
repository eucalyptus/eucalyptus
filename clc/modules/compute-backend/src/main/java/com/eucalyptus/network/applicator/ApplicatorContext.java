/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network.applicator;

import static org.hamcrest.CoreMatchers.notNullValue;
import javax.annotation.Nonnull;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.util.Parameters;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;

/**
 * Context for application
 */
public class ApplicatorContext {

  public static final TypedKey<BNetworkInfo> INFO_KEY = TypedKey.create( "NetworkInfo" );

  private final Iterable<Cluster> clusters;
  private final TypedContext resourceContext = TypedContext.newTypedContext( );

  public ApplicatorContext(
      @Nonnull final Iterable<Cluster> clusters,
      @Nonnull final BNetworkInfo networkInfo
  ) {
    Parameters.checkParam( "clusters", clusters, notNullValue( ) );
    Parameters.checkParam( "networkInfo", networkInfo, notNullValue( ) );
    this.clusters = clusters;
    setAttribute( INFO_KEY, networkInfo );
  }

  /**
   * Get the clusters for the network information.
   */
  @Nonnull
  public Iterable<Cluster> getClusters( ) {
    return clusters;
  }

  /**
   * Get the NetworkInfo being applied.
   *
   * @return The network information.
   */
  @Nonnull
  public BNetworkInfo getNetworkInfo( ) {
    return getAttribute( INFO_KEY );
  }

  /**
   * Get a typesafe attribute from the context.
   */
  public <T> T getAttribute( final TypedKey<T> key ) {
    return resourceContext.get( key );
  }

  /**
   * Set a typesafe attribute on the context.
   */
  public <T> T setAttribute( final TypedKey<T> key, final T value ) {
    return resourceContext.put( key, value );
  }

  /**
   * Remove a typesafe attribute from the context.
   */
  public <T> T removeAttribute( final TypedKey<T> key ) {
    return resourceContext.remove( key );
  }

}
