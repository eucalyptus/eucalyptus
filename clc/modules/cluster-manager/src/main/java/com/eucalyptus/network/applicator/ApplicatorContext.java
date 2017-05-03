/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.network.applicator;

import static org.hamcrest.CoreMatchers.notNullValue;
import javax.annotation.Nonnull;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.util.Parameters;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;

/**
 * Context for application
 */
public class ApplicatorContext {

  private final Iterable<Cluster> clusters;
  private final NetworkInfo networkInfo;
  private final TypedContext resourceContext = TypedContext.newTypedContext( );

  public ApplicatorContext(
      @Nonnull final Iterable<Cluster> clusters,
      @Nonnull final NetworkInfo networkInfo
  ) {
    Parameters.checkParam( "clusters", clusters, notNullValue( ) );
    Parameters.checkParam( "networkInfo", networkInfo, notNullValue( ) );
    this.clusters = clusters;
    this.networkInfo = networkInfo;
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
  public NetworkInfo getNetworkInfo( ) {
    return networkInfo;
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
