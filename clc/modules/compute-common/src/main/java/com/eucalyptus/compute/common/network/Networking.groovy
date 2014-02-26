/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.network

import com.google.common.collect.Iterators
import com.google.common.collect.Sets
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * Client for networking service (with majesty)
 */
@CompileStatic
class Networking {

  private static final Networking networking = new Networking( )
  private final NetworkingService service =
      Iterators.get( ServiceLoader.load( NetworkingService.class ).iterator(), 0 )

  static Networking getInstance( ) {
    networking
  }

  boolean supports( final NetworkingFeature feature ) {
    describeFeatures( ).contains( feature )
  }

  @Nonnull
  Set<NetworkingFeature> describeFeatures( ) {
    Sets.newHashSet( service.describeFeatures( new DescribeNetworkingFeaturesType( ) )
        ?.describeNetworkingFeaturesResult?.networkingFeatures?:[] )
  }

  PrepareNetworkResourcesResultType prepare( final PrepareNetworkResourcesType request ) {
    return service.prepare( request ).prepareNetworkResourcesResultType
  }

  void release( final ReleaseNetworkResourcesType releaseNetworkResourcesType ) {
    service.release( releaseNetworkResourcesType )
  }

  void update( final UpdateNetworkResourcesType updateNetworkResourcesType ) {
    service.update( updateNetworkResourcesType )
  }

  void update( final UpdateInstanceResourcesType updateInstanceResourcesType ) {
    service.update( updateInstanceResourcesType )
  }
}
