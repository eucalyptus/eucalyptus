/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

  boolean update( final UpdateInstanceResourcesType updateInstanceResourcesType ) {
    service.update( updateInstanceResourcesType )?.updated ?: false
  }
}
